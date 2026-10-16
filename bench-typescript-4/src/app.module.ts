import { BullModule } from '@nestjs/bullmq';
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { ScheduleModule } from '@nestjs/schedule';
import { TypeOrmModule } from '@nestjs/typeorm';
import Redis from 'ioredis';
import configuration from './config/configuration';
import { ConsultationsController } from './consultations/consultations.controller';
import { ConsultationsService } from './consultations/consultations.service';
import { AvailabilitySlot, Clinician, Consultation, Recording } from './entities/consultation.entity';
import { PatientCredit, Payment } from './entities/payment.entity';
import { PharmacyOrder } from './entities/pharmacy-order.entity';
import { Prescription, PrescriptionItem } from './entities/prescription.entity';
import { RemindersCron } from './jobs/reminders.cron';
import { RemindersService } from './jobs/reminders.service';
import { PatientCreditService } from './payments/patient-credit.service';
import { PaymentWebhooksController } from './payments/payment-webhooks.controller';
import { PaymentsService } from './payments/payments.service';
import { PharmacyModule } from './pharmacy/pharmacy.module';
import { PrescriptionsRepository } from './prescriptions/prescriptions.repository';
import { PrescriptionsService } from './prescriptions/prescriptions.service';
import { RecordingsService } from './recordings/recordings.service';

const entities = [Clinician, AvailabilitySlot, Consultation, Recording, Prescription, PrescriptionItem, PharmacyOrder, Payment, PatientCredit];

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, load: [configuration] }),
    ScheduleModule.forRoot(),
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        url: config.get<string>('database.url'),
        entities,
        synchronize: false,
      }),
    }),
    TypeOrmModule.forFeature(entities),
    BullModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({ connection: { url: config.get<string>('redis.url') } }),
    }),
    BullModule.registerQueue({ name: 'notifications' }, { name: 'payments' }),
    PharmacyModule,
  ],
  controllers: [ConsultationsController, PaymentWebhooksController],
  providers: [
    ConsultationsService,
    RecordingsService,
    PrescriptionsService,
    PrescriptionsRepository,
    PaymentsService,
    PatientCreditService,
    RemindersService,
    RemindersCron,
    {
      provide: 'REDIS_CLIENT',
      inject: [ConfigService],
      useFactory: (config: ConfigService) => new Redis(config.get<string>('redis.url')),
    },
  ],
})
export class AppModule {}
