import { Module } from '@nestjs/common';
import { AnalyticsClient } from './analytics/analytics.client';
import { LoanFunnelTracker } from './analytics/loan-funnel.tracker';
import { KycClient } from './onboarding/kyc.client';
import { OnboardingController } from './onboarding/onboarding.controller';
import { OnboardingService } from './onboarding/onboarding.service';
import { ProfileController } from './onboarding/profile.controller';
import { ProfileUpdateService } from './onboarding/profile-update.service';
import { StatementClient } from './statements/statement.client';
import { StatementController } from './statements/statement.controller';
import { StatementViewService } from './statements/statement-view.service';

/** Customer onboarding, profile self-service and loan statements. */
@Module({
  controllers: [OnboardingController, ProfileController, StatementController],
  providers: [
    AnalyticsClient,
    LoanFunnelTracker,
    KycClient,
    OnboardingService,
    ProfileUpdateService,
    StatementClient,
    StatementViewService,
  ],
})
export class OnboardingStatementsModule {}
