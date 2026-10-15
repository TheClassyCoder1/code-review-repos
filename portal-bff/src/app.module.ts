import { Module } from '@nestjs/common';
import { GraphQLModule } from '@nestjs/graphql';
import { ApolloDriver, ApolloDriverConfig } from '@nestjs/apollo';
import { join } from 'path';

import { HealthController } from './controllers/health.controller';
import { ProxyController } from './controllers/proxy.controller';
import { PortalController } from './controllers/portal.controller';
import { AggregationService } from './services/aggregation.service';
import { LoanClient } from './clients/loan.client';
import { RiskClient } from './clients/risk.client';
import { DashboardResolver } from './resolvers/dashboard.resolver';
import { NotifyWorkerClient } from './clients/notify-worker.client';
import { PartnerAuthGuard } from './auth/partner-auth.guard';
import { PartnerDirectory } from './auth/partner-directory.service';
import { NotificationsController } from './notifications/notifications.controller';
import { NotificationStore } from './notifications/notification.store';
import { PartnerWebhookService } from './notifications/partner-webhook.service';
import { PreferencesService } from './notifications/preferences.service';
import { DocumentsController } from './documents/documents.controller';
import { DocumentLinkService } from './documents/document-link.service';

@Module({
  imports: [
    GraphQLModule.forRoot<ApolloDriverConfig>({
      driver: ApolloDriver,
      autoSchemaFile: join(process.cwd(), 'src/schema.gql'),
    }),
  ],
  controllers: [HealthController, ProxyController, PortalController, NotificationsController, DocumentsController],
  providers: [
    AggregationService,
    LoanClient,
    RiskClient,
    DashboardResolver,
    NotifyWorkerClient,
    NotificationStore,
    PartnerDirectory,
    PartnerAuthGuard,
    PartnerWebhookService,
    PreferencesService,
    DocumentLinkService,
  ],
})
export class AppModule {}
