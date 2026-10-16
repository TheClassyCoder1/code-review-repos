import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { PatientCredit } from '../entities/payment.entity';

@Injectable()
export class PatientCreditService {
  constructor(@InjectRepository(PatientCredit) private readonly credits: Repository<PatientCredit>) {}

  async getBalance(patientId: string): Promise<number> {
    const credit = await this.credits.findOneBy({ patientId });
    return credit?.balanceCents ?? 0;
  }

  async adjustCreditBalance(manager: EntityManager, patientId: string, deltaCents: number): Promise<PatientCredit> {
    const credit = await manager.findOneBy(PatientCredit, { patientId });
    if (!credit) {
      throw new NotFoundException('Patient has no credit account');
    }
    const next = credit.balanceCents + deltaCents;
    if (next < 0) {
      throw new BadRequestException('Insufficient credit balance');
    }
    credit.balanceCents = next;
    return manager.save(credit);
  }
}
