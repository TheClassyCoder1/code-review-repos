import { InjectQueue } from '@nestjs/bullmq';
import { Inject, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Queue } from 'bullmq';
import Redis from 'ioredis';
import { randomUUID } from 'node:crypto';
import { Between, IsNull, Repository } from 'typeorm';
import { Consultation, ConsultationStatus } from '../entities/consultation.entity';
import { RemindersService } from './reminders.service';

const LOCK_KEY = 'locks:consultation-reminders';
const RELEASE_LOCK = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

@Injectable()
export class RemindersCron {
  private readonly logger = new Logger(RemindersCron.name);

  constructor(
    @InjectRepository(Consultation) private readonly consultations: Repository<Consultation>,
    @InjectQueue('notifications') private readonly notifications: Queue,
    @Inject('REDIS_CLIENT') private readonly redis: Redis,
    private readonly reminders: RemindersService,
    private readonly config: ConfigService,
  ) {}

  @Cron(CronExpression.EVERY_MINUTE)
  async sendUpcomingReminders(): Promise<void> {
    const token = randomUUID();
    const acquired = await this.redis.set(LOCK_KEY, token, 'PX', this.config.get<number>('reminders.lockTtlMs'), 'NX');
    if (acquired !== 'OK') {
      return;
    }
    try {
      const now = new Date();
      const windowEnd = new Date(now.getTime() + this.config.get<number>('reminders.leadMinutes') * 60_000);
      const due = await this.consultations.find({
        where: { status: ConsultationStatus.SCHEDULED, reminderSentAt: IsNull(), startsAt: Between(now, windowEnd) },
        relations: { clinician: true },
        order: { startsAt: 'ASC' },
        take: 500,
      });
      let sent = 0;
      for (const consultation of due) {
        if (!(await this.reminders.claimReminder(consultation.id))) {
          continue;
        }
        await this.notifications.add(
          'sms',
          { consultationId: consultation.id, to: consultation.patientPhone, body: this.reminders.buildReminderText(consultation) },
          { attempts: 3, backoff: { type: 'exponential', delay: 5_000 } },
        );
        sent += 1;
      }
      this.logger.log(`Queued ${sent} consultation reminders`);
    } finally {
      await this.redis.eval(RELEASE_LOCK, 1, LOCK_KEY, token);
    }
  }
}
