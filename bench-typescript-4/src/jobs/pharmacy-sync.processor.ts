import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Job } from 'bullmq';
import { LessThan, Repository } from 'typeorm';
import { PharmacyOrder, PharmacyOrderStatus } from '../entities/pharmacy-order.entity';
import { PharmacyClient, RemoteOrderStatus } from '../pharmacy/pharmacy.client';

const REMOTE_STATUS_MAP: Record<RemoteOrderStatus, PharmacyOrderStatus | null> = {
  received: null,
  filling: null,
  ready: PharmacyOrderStatus.READY,
  shipped: PharmacyOrderStatus.SHIPPED,
  delivered: PharmacyOrderStatus.DELIVERED,
  cancelled: PharmacyOrderStatus.CANCELLED,
};

@Processor('pharmacy-sync', { concurrency: 4 })
export class PharmacySyncProcessor extends WorkerHost {
  private readonly logger = new Logger(PharmacySyncProcessor.name);

  constructor(
    @InjectRepository(PharmacyOrder) private readonly orders: Repository<PharmacyOrder>,
    private readonly pharmacy: PharmacyClient,
    private readonly config: ConfigService,
  ) {
    super();
  }

  async process(job: Job<{ orderId: string }>): Promise<PharmacyOrderStatus | null> {
    if (job.name !== 'sync-status') {
      this.logger.warn(`Unknown pharmacy-sync job ${job.name}`);
      return null;
    }
    return this.syncOrderStatus(job.data.orderId);
  }

  async syncOrderStatus(orderId: string): Promise<PharmacyOrderStatus | null> {
    const order = await this.orders.findOneBy({ id: orderId });
    if (!order?.externalRef) {
      return null;
    }
    const remote = await this.pharmacy.getOrderStatus(order.externalRef);
    const mapped = REMOTE_STATUS_MAP[remote];
    if (mapped && mapped !== order.status) {
      await this.orders.update({ id: order.id, status: order.status }, { status: mapped });
      return mapped;
    }
    return order.status;
  }

  private unpaidCutoff(): Date {
    const ttlMinutes = this.config.get<number>('pharmacy.unpaidOrderTtlMinutes');
    return new Date(Date.now() - ttlMinutes * 60_000);
  }

  @Cron(CronExpression.EVERY_10_MINUTES)
  async expireStaleOrders(): Promise<void> {
    const stale = await this.orders.find({
      where: { status: PharmacyOrderStatus.AWAITING_PAYMENT, createdAt: LessThan(this.unpaidCutoff()) },
      take: 200,
    });
    stale.forEach(async (order) => {
      if (order.externalRef) {
        await this.pharmacy.cancelOrder(order.externalRef);
      }
      await this.orders.update({ id: order.id, status: PharmacyOrderStatus.AWAITING_PAYMENT }, { status: PharmacyOrderStatus.EXPIRED });
    });
    this.logger.log(`Expired ${stale.length} unpaid pharmacy orders`);
  }
}
