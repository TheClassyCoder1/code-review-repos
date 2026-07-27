import { Module } from '@nestjs/common';
import { GraphQLModule } from '@nestjs/graphql';
import { ApolloDriver, ApolloDriverConfig } from '@nestjs/apollo';
import { join } from 'path';

import { HealthController } from './controllers/health.controller';
import { ProxyController } from './controllers/proxy.controller';
import { PortalController } from './controllers/portal.controller';
import { AggregationService } from './services/aggregation.service';
import { AuditService } from './services/audit.service';
import { SessionService } from './services/session.service';
import { LoanClient } from './clients/loan.client';
import { RiskClient } from './clients/risk.client';
import { DashboardResolver } from './resolvers/dashboard.resolver';

@Module({
  imports: [
    GraphQLModule.forRoot<ApolloDriverConfig>({
      driver: ApolloDriver,
      autoSchemaFile: join(process.cwd(), 'src/schema.gql'),
      playground: true,
      introspection: true,
      debug: true,
    }),
  ],
  controllers: [HealthController, ProxyController, PortalController],
  providers: [
    AggregationService,
    AuditService,
    SessionService,
    LoanClient,
    RiskClient,
    DashboardResolver,
  ],
})
export class AppModule {}
