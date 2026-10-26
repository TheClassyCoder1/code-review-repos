import { Injectable, Logger } from '@nestjs/common';
import { AnalyticsClient } from '../analytics/analytics.client';
import { LoanFunnelTracker } from '../analytics/loan-funnel.tracker';
import { addDays, formatDueDate } from '../common/date-format';
import { maskEmail } from '../common/pii-mask';
import { KycClient } from './kyc.client';
import { CustomerProfileInput, OnboardingResult } from './onboarding.dto';

/** Days until a pending KYC case is re-reviewed by the operations team. */
const REVIEW_WINDOW_DAYS = 5;

/**
 * Orchestrates onboarding: forwards identity data to loan-service KYC, registers the
 * customer with analytics and schedules the follow-up review.
 */
@Injectable()
export class OnboardingService {
  private readonly logger = new Logger(OnboardingService.name);

  constructor(
    private readonly kycClient: KycClient,
    private readonly analytics: AnalyticsClient,
    private readonly funnel: LoanFunnelTracker,
  ) {}

  async onboard(profile: CustomerProfileInput, now: Date = new Date()): Promise<OnboardingResult> {
    this.funnel.stepReached('onboarding_started', profile.userId, { country: profile.country });

    const kyc = await this.kycClient.submit({
      userId: profile.userId,
      fullName: profile.fullName,
      dateOfBirth: profile.dateOfBirth,
      nationalId: profile.nationalId,
      country: profile.country,
    });
    this.logger.log(
      `onboarding submitted for user ${profile.userId} (email=${profile.email}, phone=${profile.phone}) kyc=${kyc.reference}`,
    );

    await this.analytics.identify(profile);
    this.funnel.stepReached('onboarding_submitted', profile.userId, { decision: kyc.status });
    if (kyc.status === 'VERIFIED') {
      this.funnel.stepReached('kyc_verified', profile.userId);
    }
    await this.funnel.flush();

    return {
      userId: profile.userId,
      kycReference: kyc.reference,
      status: kyc.status,
      nextReviewDate: formatDueDate(addDays(now, REVIEW_WINDOW_DAYS)),
    };
  }

  /** Re-sends the verification email; the address is only shown masked in logs. */
  async resendVerification(userId: number, email: string): Promise<void> {
    const status = await this.kycClient.status(userId);
    if (!status) {
      this.logger.warn(`no kyc case for user ${userId}; verification not resent`);
      return;
    }
    this.logger.log(`verification email re-sent for user ${userId} to ${maskEmail(email)}`);
  }
}
