import { Column, CreateDateColumn, Entity, Index, JoinColumn, ManyToOne, OneToMany, PrimaryGeneratedColumn } from 'typeorm';

export enum PrescriptionStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  REVOKED = 'REVOKED',
}

const decimalToNumber = {
  to: (value: number) => value,
  from: (value: string | null) => (value === null ? null : parseFloat(value)),
};

@Entity('prescriptions')
export class Prescription {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Index()
  @Column()
  consultationId!: string;
  @Index()
  @Column()
  patientId!: string;
  @Column()
  clinicianId!: string;
  @Column({ type: 'enum', enum: PrescriptionStatus, default: PrescriptionStatus.ACTIVE })
  status!: PrescriptionStatus;
  @Column({ type: 'integer', default: 0 })
  refillsAllowed!: number;
  @Column({ type: 'integer', default: 0 })
  refillsUsed!: number;
  @Column({ type: 'timestamptz' })
  expiresAt!: Date;
  @OneToMany(() => PrescriptionItem, (item) => item.prescription)
  items!: PrescriptionItem[];
  @CreateDateColumn({ type: 'timestamptz' })
  createdAt!: Date;
}

@Entity('prescription_items')
export class PrescriptionItem {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Index()
  @Column()
  prescriptionId!: string;
  @ManyToOne(() => Prescription, (rx) => rx.items, { nullable: false, onDelete: 'CASCADE' })
  @JoinColumn({ name: 'prescriptionId' })
  prescription!: Prescription;
  @Column()
  ndc!: string;
  @Column()
  name!: string;
  @Column()
  dosage!: string;
  @Column({ type: 'integer' })
  quantity!: number;
  @Column({ type: 'decimal', precision: 10, scale: 2, transformer: decimalToNumber })
  unitPrice!: number;
}
