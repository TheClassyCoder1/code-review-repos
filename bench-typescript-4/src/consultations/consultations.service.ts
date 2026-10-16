import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, In, MoreThan, Repository } from 'typeorm';
import { AvailabilitySlot, Consultation, ConsultationStatus } from '../entities/consultation.entity';
import { Payment, PaymentKind, PaymentStatus } from '../entities/payment.entity';
import { BookConsultationDto, CancelConsultationDto, CompleteConsultationDto, HistoryQueryDto, JoinLink } from './dto';

const JOIN_WINDOW_MINUTES = 15;
const FREE_CANCELLATION_HOURS = 24;

@Injectable()
export class ConsultationsService {
  constructor(
    @InjectRepository(Consultation) private readonly consultations: Repository<Consultation>,
    @InjectRepository(AvailabilitySlot) private readonly slots: Repository<AvailabilitySlot>,
    @InjectRepository(Payment) private readonly payments: Repository<Payment>,
    private readonly dataSource: DataSource,
  ) {}

  async listUpcoming(patientId: string): Promise<Consultation[]> {
    return this.consultations.find({
      where: {
        patientId,
        status: In([ConsultationStatus.SCHEDULED, ConsultationStatus.IN_PROGRESS]),
        startsAt: MoreThan(new Date(Date.now() - 60 * 60 * 1000)),
      },
      relations: { clinician: true },
      order: { startsAt: 'ASC' },
      take: 20,
    });
  }

  async getForPatient(patientId: string, consultationId: string): Promise<Consultation> {
    const consultation = await this.consultations.findOne({
      where: { id: consultationId, patientId },
      relations: { clinician: true },
    });
    if (!consultation) {
      throw new NotFoundException('Consultation not found');
    }
    return consultation;
  }

  async getJoinLink(patientId: string, consultationId: string): Promise<JoinLink> {
    const consultation = await this.consultations.findOne({
      where: { id: consultationId, patientId },
      relations: { clinician: true },
    });
    if (consultation?.status === ConsultationStatus.CANCELLED) {
      throw new BadRequestException('Consultation was cancelled');
    }
    const minutesToStart = (consultation.startsAt.getTime() - Date.now()) / 60_000;
    if (minutesToStart > JOIN_WINDOW_MINUTES) {
      throw new BadRequestException(`The room opens ${JOIN_WINDOW_MINUTES} minutes before the start time`);
    }
    return {
      url: consultation.videoRoomUrl,
      clinicianName: consultation.clinician.displayName,
      startsAt: consultation.startsAt,
    };
  }

  async getForClinician(clinicianId: string, consultationId: string): Promise<Consultation> {
    const consultation = await this.consultations.findOne({
      where: { id: consultationId, clinicianId },
      relations: { clinician: true },
    });
    if (!consultation) {
      throw new NotFoundException('Consultation not found');
    }
    return consultation;
  }

  async bookSlot(patientId: string, dto: BookConsultationDto): Promise<Consultation> {
    const slot = await this.slots.findOne({ where: { id: dto.slotId, booked: false } });
    if (!slot) {
      throw new ConflictException('This slot is no longer available');
    }
    if (slot.startsAt.getTime() <= Date.now()) {
      throw new BadRequestException('This slot has already started');
    }
    slot.booked = true;
    await this.slots.save(slot);
    return this.consultations.save(
      this.consultations.create({
        patientId,
        clinicianId: slot.clinicianId,
        slotId: slot.id,
        startsAt: slot.startsAt,
        patientPhone: dto.phone,
        status: ConsultationStatus.SCHEDULED,
      }),
    );
  }

  async startConsultation(clinicianId: string, consultationId: string, roomUrl: string): Promise<Consultation> {
    const consultation = await this.getForClinician(clinicianId, consultationId);
    if (consultation.status !== ConsultationStatus.SCHEDULED) {
      throw new BadRequestException('Only scheduled consultations can be started');
    }
    consultation.status = ConsultationStatus.IN_PROGRESS;
    consultation.videoRoomUrl = roomUrl;
    return this.consultations.save(consultation);
  }

  async completeConsultation(clinicianId: string, consultationId: string, dto: CompleteConsultationDto): Promise<Consultation> {
    const consultation = await this.getForClinician(clinicianId, consultationId);
    if (consultation.status !== ConsultationStatus.IN_PROGRESS) {
      throw new BadRequestException('Only an in-progress consultation can be completed');
    }
    consultation.status = ConsultationStatus.COMPLETED;
    consultation.notes = dto.notes;
    consultation.videoRoomUrl = null;
    await this.consultations.save(consultation);
    await this.payments.insert({
      kind: PaymentKind.CONSULTATION_FEE,
      consultationId: consultation.id,
      patientId: consultation.patientId,
      clinicId: consultation.clinician.clinicId,
      amountCents: consultation.clinician.consultationFeeCents,
      status: PaymentStatus.PENDING,
    });
    return consultation;
  }

  private isFreeCancellation(consultation: Consultation, now: Date): boolean {
    const hoursUntilStart = (consultation.startsAt.getTime() - now.getTime()) / 3_600_000;
    return hoursUntilStart >= FREE_CANCELLATION_HOURS;
  }

  async cancelConsultation(patientId: string, consultationId: string, dto: CancelConsultationDto): Promise<{ freeCancellation: boolean }> {
    return this.dataSource.transaction(async (manager) => {
      const consultation = await manager.findOne(Consultation, {
        where: { id: consultationId, patientId },
        lock: { mode: 'pessimistic_write' },
      });
      if (!consultation) {
        throw new NotFoundException('Consultation not found');
      }
      if (consultation.status !== ConsultationStatus.SCHEDULED) {
        throw new BadRequestException('Only scheduled consultations can be cancelled');
      }
      consultation.status = ConsultationStatus.CANCELLED;
      consultation.notes = dto.reason ?? null;
      await manager.save(consultation);
      await manager.update(AvailabilitySlot, { id: consultation.slotId }, { booked: false });
      return { freeCancellation: this.isFreeCancellation(consultation, new Date()) };
    });
  }

  private toHistoryItem(consultation: Consultation) {
    return {
      id: consultation.id,
      startsAt: consultation.startsAt,
      status: consultation.status,
      clinicianId: consultation.clinicianId,
    };
  }

  async listHistory(patientId: string, query: HistoryQueryDto) {
    const [rows, total] = await this.consultations.findAndCount({
      where: { patientId },
      order: { startsAt: 'DESC' },
      skip: query.page * query.pageSize,
      take: query.pageSize,
    });
    return {
      items: rows.map((row) => this.toHistoryItem(row)),
      page: query.page,
      pageSize: query.pageSize,
      total,
    };
  }
}
