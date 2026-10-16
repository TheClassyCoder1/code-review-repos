import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';

export enum ConsultationStatus {
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

@Entity('clinicians')
export class Clinician {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Column()
  displayName!: string;
  @Column({ default: 'UTC' })
  timezone!: string;
  @Column()
  clinicId!: string;
  @Column({ type: 'integer' })
  consultationFeeCents!: number;
}

@Entity('availability_slots')
@Index(['clinicianId', 'startsAt'])
export class AvailabilitySlot {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Column()
  clinicianId!: string;
  @Column({ type: 'timestamptz' })
  startsAt!: Date;
  @Column({ type: 'timestamptz' })
  endsAt!: Date;
  @Column({ default: false })
  booked!: boolean;
}

@Entity('consultations')
export class Consultation {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Index()
  @Column()
  patientId!: string;
  @Column()
  clinicianId!: string;
  @ManyToOne(() => Clinician, { nullable: false })
  @JoinColumn({ name: 'clinicianId' })
  clinician!: Clinician;
  @Column()
  slotId!: string;
  @Column({ type: 'timestamptz' })
  startsAt!: Date;
  @Column({ type: 'enum', enum: ConsultationStatus, default: ConsultationStatus.SCHEDULED })
  status!: ConsultationStatus;
  @Column({ type: 'varchar', nullable: true })
  videoRoomUrl!: string | null;
  @Column()
  patientPhone!: string;
  @Column({ type: 'text', nullable: true })
  notes!: string | null;
  @Column({ type: 'timestamptz', nullable: true })
  reminderSentAt!: Date | null;
  @CreateDateColumn({ type: 'timestamptz' })
  createdAt!: Date;
  @UpdateDateColumn({ type: 'timestamptz' })
  updatedAt!: Date;
}

@Entity('recordings')
export class Recording {
  @PrimaryGeneratedColumn('uuid')
  id!: string;
  @Index()
  @Column()
  consultationId!: string;
  @ManyToOne(() => Consultation, { nullable: false })
  @JoinColumn({ name: 'consultationId' })
  consultation!: Consultation;
  @Column()
  storageKey!: string;
  @Column({ type: 'varchar', nullable: true })
  transcriptKey!: string | null;
  @Column({ type: 'integer' })
  durationSeconds!: number;
  @Column({ type: 'bigint' })
  sizeBytes!: string;
  @CreateDateColumn({ type: 'timestamptz' })
  createdAt!: Date;
}
