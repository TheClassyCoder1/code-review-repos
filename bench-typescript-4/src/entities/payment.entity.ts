import { Column, CreateDateColumn, Entity, Index, PrimaryColumn, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

export enum PaymentKind {
  CONSULTATION_FEE = 'CONSULTATION_FEE',
  PHARMACY_ORDER = 'PHARMACY_ORDER',
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  AUTHORIZED = 'AUTHORIZED',
  CAPTURED = 'CAPTURED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
}

@Entity('payments')
@Index(['clinicId', 'createdAt'])
export class Payment {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Column({ type: 'enum', enum: PaymentKind })
  kind!: PaymentKind;
  @Column({ type: 'varchar', nullable: true })
  consultationId!: string | null;
  @Column({ type: 'varchar', nullable: true, unique: true })
  pharmacyOrderId!: string | null;
  @Column()
  patientId!: string;
  @Column()
  clinicId!: string;
  @Column({ type: 'integer' }) amountCents!: number;
  @Column({ type: 'integer', default: 0 })
  refundedCents!: number;
  @Column({ length: 3, default: 'USD' })
  currency!: string;
  @Column({ type: 'enum', enum: PaymentStatus, default: PaymentStatus.PENDING })
  status!: PaymentStatus;
  @Index({ unique: true, where: '"gatewayChargeId" IS NOT NULL' })
  @Column({ type: 'varchar', nullable: true })
  gatewayChargeId!: string | null;
  @Column({ type: 'timestamptz', nullable: true })
  capturedAt!: Date | null;
  @CreateDateColumn({ type: 'timestamptz' })
  createdAt!: Date;
}

@Entity('patient_credits')
export class PatientCredit {
  @PrimaryColumn()
  patientId!: string;
  @Column({ type: 'integer', default: 0 })
  balanceCents!: number;
  @UpdateDateColumn({ type: 'timestamptz' })
  updatedAt!: Date;
}
