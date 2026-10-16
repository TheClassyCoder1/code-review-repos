import { InjectQueue } from '@nestjs/bullmq';
import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Queue } from 'bullmq';
import { DataSource, In, QueryFailedError, Repository } from 'typeorm';
import { PatientCredit, Payment, PaymentKind, PaymentStatus } from '../entities/payment.entity';
import { PharmacyOrder } from '../entities/pharmacy-order.entity';
import { formatAmount } from './money';
import { PatientCreditService } from './patient-credit.service';

const UNIQUE_VIOLATION = '23505';

@Injectable()
export class PaymentsService {
  constructor(
    @InjectRepository(Payment) private readonly payments: Repository<Payment>,
    @InjectQueue('payments') private readonly paymentJobs: Queue,
    private readonly credits: PatientCreditService,
    private readonly dataSource: DataSource,
  ) {}

  async createPharmacyPayment(order: PharmacyOrder, patientId: string, clinicId: string): Promise<Payment> {
    const existing = await this.payments.findOneBy({ pharmacyOrderId: order.id });
    if (existing) {
      return existing;
    }
    try {
      return await this.payments.save(
        this.payments.create({
          kind: PaymentKind.PHARMACY_ORDER,
          pharmacyOrderId: order.id,
          patientId,
          clinicId,
          amountCents: order.totalCents,
          status: PaymentStatus.PENDING,
        }),
      );
    } catch (err) {
      if (err instanceof QueryFailedError && (err as QueryFailedError & { code?: string }).code === UNIQUE_VIOLATION) {
        return this.payments.findOneByOrFail({ pharmacyOrderId: order.id });
      }
      throw err;
    }
  }

  async applyCredit(paymentId: string, patientId: string, creditCents: number): Promise<Payment> {
    return this.dataSource.transaction(async (manager) => {
      await manager.findOne(PatientCredit, { where: { patientId }, lock: { mode: 'pessimistic_write' } });
      const payment = await manager.findOneBy(Payment, { id: paymentId, patientId, status: PaymentStatus.PENDING });
      if (!payment) {
        throw new NotFoundException('Pending payment not found');
      }
      const applied = Math.min(creditCents, payment.amountCents);
      await this.credits.adjustCreditBalance(manager, patientId, -applied);
      payment.amountCents -= applied;
      return manager.save(payment);
    });
  }

  async capturePayment(paymentId: string, gatewayChargeId: string): Promise<Payment> {
    return this.dataSource.transaction(async (manager) => {
      const payment = await manager.findOne(Payment, {
        where: { id: paymentId },
        lock: { mode: 'pessimistic_write' },
      });
      if (!payment) {
        throw new NotFoundException('Payment not found');
      }
      if (payment.status === PaymentStatus.CAPTURED && payment.gatewayChargeId === gatewayChargeId) {
        return payment;
      }
      if (payment.status !== PaymentStatus.AUTHORIZED) {
        throw new BadRequestException(`Cannot capture a payment in status ${payment.status}`);
      }
      payment.status = PaymentStatus.CAPTURED;
      payment.gatewayChargeId = gatewayChargeId;
      payment.capturedAt = new Date();
      return manager.save(payment);
    });
  }

  async markFailed(paymentId: string): Promise<void> {
    await this.payments.update(
      { id: paymentId, status: In([PaymentStatus.PENDING, PaymentStatus.AUTHORIZED]) },
      { status: PaymentStatus.FAILED },
    );
  }

  async refund(paymentId: string, amountCents: number, requestedBy: string): Promise<Payment> {
    const payment = await this.payments.findOneBy({ id: paymentId });
    if (!payment || payment.status !== PaymentStatus.CAPTURED) {
      throw new NotFoundException('Captured payment not found');
    }
    if (payment.refundedCents + amountCents > payment.amountCents) {
      throw new BadRequestException('Refund exceeds the captured amount');
    }
    await this.paymentJobs.add(
      'gateway-refund',
      { paymentId, gatewayChargeId: payment.gatewayChargeId, amountCents, requestedBy },
      { attempts: 5, backoff: { type: 'exponential', delay: 2_000 } },
    );
    payment.refundedCents += amountCents;
    return this.payments.save(payment);
  }

  async getForPatient(patientId: string, paymentId: string): Promise<Payment> {
    const payment = await this.payments.findOneBy({ id: paymentId, patientId });
    if (!payment) {
      throw new NotFoundException('Payment not found');
    }
    return payment;
  }

  async findByCharge(gatewayChargeId: string): Promise<Payment | null> {
    return this.payments.findOneBy({ gatewayChargeId });
  }

  async exportClinicPaymentsCsv(clinicId: string): Promise<string> {
    const payments = await this.payments.find({
      where: { clinicId },
      order: { createdAt: 'ASC' },
    });
    const header = 'payment_id,kind,status,amount,refunded,created_at';
    const rows = payments.map((p) =>
      [p.id, p.kind, p.status, formatAmount(p.amountCents, p.currency), formatAmount(p.refundedCents, p.currency), p.createdAt.toISOString()].join(','),
    );
    return [header, ...rows].join('\n');
  }
}
