import { Injectable } from '@nestjs/common';
import { EntityManager } from 'typeorm';
import { Prescription, PrescriptionItem, PrescriptionStatus } from '../entities/prescription.entity';
import { PrescriptionItemDto } from './dto';

export interface NewPrescription {
  consultationId: string;
  patientId: string;
  clinicianId: string;
  refillsAllowed: number;
  expiresAt: Date;
  items: PrescriptionItemDto[];
}

@Injectable()
export class PrescriptionsRepository {
  async findActiveForPatient(manager: EntityManager, patientId: string): Promise<Prescription[]> {
    return manager.find(Prescription, {
      where: { patientId, status: PrescriptionStatus.ACTIVE },
      order: { createdAt: 'DESC' },
      take: 50,
    });
  }

  async writePrescriptionWithItems(manager: EntityManager, input: NewPrescription): Promise<Prescription> {
    const prescription = await manager.save(
      manager.create(Prescription, {
        consultationId: input.consultationId,
        patientId: input.patientId,
        clinicianId: input.clinicianId,
        refillsAllowed: input.refillsAllowed,
        refillsUsed: 0,
        expiresAt: input.expiresAt,
        status: PrescriptionStatus.ACTIVE,
      }),
    );
    prescription.items = await manager.save(
      input.items.map((item) => manager.create(PrescriptionItem, { ...item, prescriptionId: prescription.id })),
    );
    return prescription;
  }
}
