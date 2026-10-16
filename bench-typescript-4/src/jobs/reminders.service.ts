import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Consultation } from '../entities/consultation.entity';

@Injectable()
export class RemindersService {
  constructor(@InjectRepository(Consultation) private readonly consultations: Repository<Consultation>) {}

  buildReminderText(consultation: Consultation): string {
    const startsAt = new Intl.DateTimeFormat('en-US', {
      timeZone: consultation.clinician.timezone,
      hour: 'numeric',
      minute: '2-digit',
      timeZoneName: 'short',
    }).format(consultation.startsAt);
    return `Reminder: your video visit with ${consultation.clinician.displayName} starts at ${startsAt}. Open the app to join.`;
  }

  private minutesUntil(date: Date, now: Date): number {
    return Math.round((date.getTime() - now.getTime()) / 60_000);
  }

  isDue(consultation: Consultation, leadMinutes: number, now: Date = new Date()): boolean {
    const minutes = this.minutesUntil(consultation.startsAt, now);
    return minutes >= 0 && minutes <= leadMinutes;
  }

  async claimReminder(consultationId: string): Promise<boolean> {
    const consultation = await this.consultations.findOneBy({ id: consultationId });
    if (!consultation || consultation.reminderSentAt) {
      return false;
    }
    consultation.reminderSentAt = new Date();
    await this.consultations.save(consultation);
    return true;
  }
}
