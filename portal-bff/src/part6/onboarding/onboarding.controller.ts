import { BadRequestException, Body, Controller, HttpCode, Param, Post } from '@nestjs/common';
import {
  CustomerProfileInput,
  OnboardingResult,
  ProfileValidationError,
  SUPPORTED_COUNTRIES,
} from './onboarding.dto';
import { OnboardingService } from './onboarding.service';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\+?[0-9 ()-]{7,20}$/;

/**
 * Onboarding endpoints for the portal:
 *  - POST /portal/onboarding                      capture profile + start KYC
 *  - POST /portal/onboarding/:userId/verification resend the verification email
 */
@Controller('portal/onboarding')
export class OnboardingController {
  constructor(private readonly onboarding: OnboardingService) {}

  @Post()
  async start(@Body() body: CustomerProfileInput): Promise<OnboardingResult> {
    try {
      this.validate(body);
    } catch (err) {
      if (err instanceof ProfileValidationError) {
        throw new BadRequestException({ field: err.field, message: err.message });
      }
      throw err;
    }
    if (!body.nationalId || body.nationalId.trim().length < 6) {
      throw new BadRequestException({ field: 'nationalId', message: 'national id is required' });
    }
    return this.onboarding.onboard(body);
  }

  @Post(':userId/verification')
  @HttpCode(202)
  async resend(@Param('userId') userId: string, @Body() body: { email: string }): Promise<void> {
    await this.onboarding.resendVerification(Number(userId), body.email);
  }

  private validate(profile: CustomerProfileInput): void {
    if (!profile.fullName || profile.fullName.trim().length < 2) {
      throw new ProfileValidationError('fullName', 'full name must have at least 2 characters');
    }
    if (profile.fullName.length > 120) {
      throw new ProfileValidationError('fullName', 'full name must not exceed 120 characters');
    }
    if (!profile.email || !EMAIL_PATTERN.test(profile.email)) {
      throw new ProfileValidationError('email', 'email address is not valid');
    }
    if (!profile.phone || !PHONE_PATTERN.test(profile.phone)) {
      throw new ProfileValidationError('phone', 'phone number is not valid');
    }
    const dob = new Date(profile.dateOfBirth);
    if (Number.isNaN(dob.getTime())) {
      throw new ProfileValidationError('dateOfBirth', 'date of birth is not a valid date');
    }
    const ageMs = Date.now() - dob.getTime();
    if (ageMs < 18 * 365.25 * 86_400_000) {
      throw new ProfileValidationError('dateOfBirth', 'customer must be at least 18 years old');
    }
    if (!profile.country || !SUPPORTED_COUNTRIES.has(profile.country.toUpperCase())) {
      throw new ProfileValidationError('country', 'country is not supported');
    }
  }
}
