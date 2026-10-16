import { HttpModule } from '@nestjs/axios';
import { BullModule } from '@nestjs/bullmq';
import { Module, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Payment } from '../entities/payment.entity';
import { PharmacyOrder } from '../entities/pharmacy-order.entity';
import { PharmacySyncProcessor } from '../jobs/pharmacy-sync.processor';
import { FormularyService } from './formulary.service';
import { PharmacyClient } from './pharmacy.client';
import { PharmacyOrdersService } from './pharmacy-orders.service';

@Module({
  imports: [
    HttpModule.registerAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        baseURL: config.get<string>('pharmacy.baseUrl'),
        timeout: config.get<number>('pharmacy.timeoutMs'),
        maxRedirects: 0,
      }),
    }),
    TypeOrmModule.forFeature([PharmacyOrder, Payment]),
    BullModule.registerQueue({ name: 'pharmacy-sync' }),
  ],
  providers: [PharmacyClient, PharmacyOrdersService, FormularyService, PharmacySyncProcessor],
  exports: [PharmacyOrdersService, FormularyService],
})
export class PharmacyModule implements OnModuleInit {
  constructor(private readonly formulary: FormularyService) {}

  async onModuleInit(): Promise<void> {
    await this.formulary.loadFormulary();
  }
}
