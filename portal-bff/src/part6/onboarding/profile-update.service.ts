import { Injectable, Logger } from '@nestjs/common';
import { formatIsoDay } from '../common/date-format';
import {
  CustomerProfileInput,
  CustomerProfileUpdate,
  ProfileUpdateResult,
  ProfileValidationError,
  SUPPORTED_COUNTRIES,
} from './onboarding.dto';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\+?[0-9 ()-]{7,20}$/;

/**
 * Applies customer self-service profile edits. The portal keeps the last accepted profile
 * per user so that a partial update can be validated as a whole.
 */
@Injectable()
export class ProfileUpdateService {
  private readonly logger = new Logger(ProfileUpdateService.name);
  private readonly profiles = new Map<number, CustomerProfileInput>();

  remember(profile: CustomerProfileInput): void {
    this.profiles.set(profile.userId, { ...profile });
  }

  update(change: CustomerProfileUpdate, now: Date = new Date()): ProfileUpdateResult {
    const current = this.profiles.get(change.userId);
    if (!current) {
      throw new ProfileValidationError('userId', `no profile on record for user ${change.userId}`);
    }
    const merged: CustomerProfileInput = { ...current, ...change };
    this.validate(merged);

    const updatedFields = (Object.keys(change) as (keyof CustomerProfileUpdate)[])
      .filter((key) => key !== 'userId' && change[key] !== undefined && change[key] !== current[key]);
    this.profiles.set(change.userId, merged);
    this.logger.log(`profile for user ${change.userId} updated fields=[${updatedFields.join(',')}]`);

    return { userId: change.userId, updatedFields, updatedAt: formatIsoDay(now) };
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
