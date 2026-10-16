import { Column, CreateDateColumn, Entity, Index, JoinColumn, ManyToOne, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';
import { Prescription } from './prescription.entity';

export enum PharmacyOrderStatus {
  AWAITING_PAYMENT = 'AWAITING_PAYMENT',
  PAID = 'PAID',
  SENT_TO_PHARMACY = 'SENT_TO_PHARMACY',
  READY = 'READY',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

@Entity('pharmacy_orders')
@Index(['status', 'createdAt'])
export class PharmacyOrder {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Column()
  prescriptionId!: string;
  @ManyToOne(() => Prescription, { nullable: false })
  @JoinColumn({ name: 'prescriptionId' })
  prescription!: Prescription;
  @Column()
  storeId!: string;
  @Column({ type: 'enum', enum: PharmacyOrderStatus, default: PharmacyOrderStatus.AWAITING_PAYMENT })
  status!: PharmacyOrderStatus;
  @Column({ type: 'varchar', nullable: true })
  externalRef!: string | null;
  @Column({ type: 'integer' })
  totalCents!: number;
  @Column({ type: 'jsonb' })
  deliveryAddress!: Record<string, string>;
  @Column()
  contactPhone!: string;
  @Column({ type: 'timestamptz', nullable: true })
  transmittedAt!: Date | null;
  @CreateDateColumn({ type: 'timestamptz' })
  createdAt!: Date;
  @UpdateDateColumn({ type: 'timestamptz' })
  updatedAt!: Date;
}
