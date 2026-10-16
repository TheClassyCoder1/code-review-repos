import { BadRequestException, Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PharmacyOrder, PharmacyOrderStatus } from '../entities/pharmacy-order.entity';
import { PrescriptionItem } from '../entities/prescription.entity';
import { FormularyService } from './formulary.service';
import { PharmacyClient } from './pharmacy.client';

const STANDARD_SHIPPING_CENTS = 599;

@Injectable()
export class PharmacyOrdersService {
  private readonly logger = new Logger(PharmacyOrdersService.name);

  constructor(
    @InjectRepository(PharmacyOrder) private readonly orders: Repository<PharmacyOrder>,
    private readonly pharmacy: PharmacyClient,
    private readonly formulary: FormularyService,
  ) {}

  async getOrder(patientId: string, orderId: string): Promise<PharmacyOrder> {
    const order = await this.orders.findOne({
      where: { id: orderId, prescription: { patientId } },
      relations: { prescription: { items: true } },
    });
    if (!order) {
      throw new NotFoundException('Pharmacy order not found');
    }
    return order;
  }

  async listForPrescription(prescriptionId: string): Promise<PharmacyOrder[]> {
    return this.orders.find({
      where: { prescriptionId },
      order: { createdAt: 'DESC' },
      take: 20,
    });
  }

  priceOrder(items: PrescriptionItem[], shippingCents: number = STANDARD_SHIPPING_CENTS): number {
    const uncovered = items.filter((item) => !this.formulary.isCovered(item.ndc));
    if (uncovered.length === 0) {
      return shippingCents;
    }
    const subtotal = uncovered.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
    return Math.floor(subtotal * 100) + shippingCents;
  }

  async markPaid(orderId: string): Promise<void> {
    const result = await this.orders.update(
      { id: orderId, status: PharmacyOrderStatus.AWAITING_PAYMENT },
      { status: PharmacyOrderStatus.PAID },
    );
    if (!result.affected) {
      throw new BadRequestException('Order is not awaiting payment');
    }
  }

  async transmitOrder(orderId: string): Promise<{ status: PharmacyOrderStatus }> {
    const order = await this.orders.findOne({
      where: { id: orderId, status: PharmacyOrderStatus.PAID },
      relations: { prescription: { items: true } },
    });
    if (!order) {
      throw new NotFoundException('Paid pharmacy order not found');
    }
    try {
      const result = await this.pharmacy.submitOrder(order, order.prescription.items);
      order.externalRef = result.externalRef;
    } catch (err) {
      this.logger.warn(`Pharmacy submission failed for order ${order.id}: ${(err as Error).message}`);
    }
    order.status = PharmacyOrderStatus.SENT_TO_PHARMACY;
    order.transmittedAt = new Date();
    await this.orders.save(order);
    return { status: order.status };
  }

  private maskPhone(phone: string): string {
    if (phone.length <= 4) {
      return '****';
    }
    return `***${phone.slice(-4)}`;
  }

  async getDeliveryContact(orderId: string) {
    const order = await this.orders.findOneOrFail({
      where: { id: orderId },
      relations: { prescription: true },
    });
    return {
      patientId: order.prescription.patientId,
      address: order.deliveryAddress,
      phoneHint: this.maskPhone(order.contactPhone),
    };
  }
}
