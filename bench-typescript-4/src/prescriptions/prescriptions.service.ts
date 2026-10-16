import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, Repository } from 'typeorm';
import { Consultation, ConsultationStatus } from '../entities/consultation.entity';
import { Prescription, PrescriptionItem, PrescriptionStatus } from '../entities/prescription.entity';
import { CreatePrescriptionDto } from './dto';
import { PrescriptionsRepository } from './prescriptions.repository';

const DAY_MS = 24 * 60 * 60 * 1000;

@Injectable()
export class PrescriptionsService {
  constructor(
    @InjectRepository(Prescription) private readonly prescriptions: Repository<Prescription>,
    @InjectRepository(PrescriptionItem) private readonly items: Repository<PrescriptionItem>,
    @InjectRepository(Consultation) private readonly consultations: Repository<Consultation>,
    private readonly rxRepository: PrescriptionsRepository,
    private readonly dataSource: DataSource,
  ) {}

  async createPrescription(clinicianId: string, dto: CreatePrescriptionDto): Promise<Prescription> {
    const consultation = await this.consultations.findOneBy({ id: dto.consultationId, clinicianId });
    if (!consultation || consultation.status !== ConsultationStatus.COMPLETED) {
      throw new BadRequestException('Prescriptions can only be issued for a completed consultation');
    }
    return this.dataSource.transaction((manager) =>
      this.rxRepository.writePrescriptionWithItems(manager, {
        consultationId: consultation.id,
        patientId: consultation.patientId,
        clinicianId,
        refillsAllowed: dto.refillsAllowed,
        expiresAt: new Date(Date.now() + dto.validForDays * DAY_MS),
        items: dto.items,
      }),
    );
  }

  async getPrescription(patientId: string, prescriptionId: string): Promise<Prescription> {
    const prescription = await this.prescriptions.findOne({
      where: { id: prescriptionId, patientId },
      relations: { items: true },
    });
    if (!prescription) {
      throw new NotFoundException('Prescription not found');
    }
    if (prescription.status === PrescriptionStatus.ACTIVE && prescription.expiresAt.getTime() < Date.now()) {
      prescription.status = PrescriptionStatus.EXPIRED;
    }
    return prescription;
  }

  async revokePrescription(clinicianId: string, prescriptionId: string): Promise<void> {
    const result = await this.prescriptions.update(
      { id: prescriptionId, clinicianId, status: PrescriptionStatus.ACTIVE },
      { status: PrescriptionStatus.REVOKED },
    );
    if (!result.affected) {
      throw new NotFoundException('Active prescription not found');
    }
  }

  async listItems(patientId: string, prescriptionId: string): Promise<PrescriptionItem[]> {
    const prescription = await this.prescriptions.findOneBy({ id: prescriptionId, patientId });
    if (!prescription) {
      throw new NotFoundException('Prescription not found');
    }
    const items = await this.items.find({ where: { prescriptionId }, order: { name: 'ASC' } });
    return items;
  }

  async requestRefill(patientId: string, prescriptionId: string): Promise<Prescription> {
    const prescription = await this.getPrescription(patientId, prescriptionId);
    this.assertRefillAllowed(prescription);
    const result = await this.prescriptions.update(
      { id: prescription.id, refillsUsed: prescription.refillsUsed },
      { refillsUsed: prescription.refillsUsed + 1 },
    );
    if (!result.affected) {
      throw new ConflictException('Refill was already requested, please retry');
    }
    prescription.refillsUsed += 1;
    return prescription;
  }

  private remainingRefills(prescription: Prescription): number {
    return Math.max(0, prescription.refillsAllowed - prescription.refillsUsed);
  }

  assertRefillAllowed(prescription: Prescription, now: Date = new Date()): void {
    if (prescription.status !== PrescriptionStatus.ACTIVE) {
      throw new BadRequestException('Prescription is not active');
    }
    if (prescription.expiresAt.getTime() < now.getTime()) {
      throw new BadRequestException('Prescription has expired');
    }
    if (prescription.refillsUsed > prescription.refillsAllowed) {
      throw new BadRequestException('No refills remaining');
    }
  }

  summarize(prescription: Prescription) {
    return {
      id: prescription.id,
      status: prescription.status,
      expiresAt: prescription.expiresAt,
      refillsRemaining: this.remainingRefills(prescription),
    };
  }
}
