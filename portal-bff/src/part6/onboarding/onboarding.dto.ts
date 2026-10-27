// Onboarding DTOs. A customer profile is captured once in the portal and then forwarded
// to loan-service (KYC) before the first loan application is allowed.

export interface CustomerProfileInput {
  userId: number;
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  nationalId: string;
  country: string;
  marketingConsent?: boolean;
}

export type CustomerProfileUpdate = Partial<Omit<CustomerProfileInput, 'userId' | 'nationalId'>> & {
  userId: number;
};

export interface OnboardingResult {
  userId: number;
  kycReference: string;
  status: OnboardingStatus;
  nextReviewDate: string;
}

export type OnboardingStatus = 'PENDING_REVIEW' | 'VERIFIED' | 'REJECTED';

export interface ProfileUpdateResult {
  userId: number;
  updatedFields: string[];
  updatedAt: string;
}

export interface KycSubmission {
  userId: number;
  fullName: string;
  dateOfBirth: string;
  nationalId: string;
  country: string;
}

export interface KycSubmissionResponse {
  reference: string;
  status: OnboardingStatus;
}

export class ProfileValidationError extends Error {
  constructor(readonly field: string, message: string) {
    super(message);
    this.name = 'ProfileValidationError';
  }
}

export const SUPPORTED_COUNTRIES: ReadonlySet<string> = new Set(['GB', 'IE', 'DE', 'FR', 'NL', 'ES']);
