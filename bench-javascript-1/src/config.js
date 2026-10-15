'use strict';

function required(name) {
  const value = process.env[name];
  if (value === undefined || value.trim() === '') {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value.trim();
}

module.exports = {
  port: Number(process.env.PORT || 3000),
  databaseUrl: required('DATABASE_URL'),
  jwtSecret: required('JWT_SECRET'),
  draftCookieSecret: required('DRAFT_COOKIE_SECRET'),
  uploadDir: process.env.UPLOAD_DIR || '/var/lib/clinic/uploads',
  formsDir: process.env.FORMS_DIR || '/var/lib/clinic/forms',
  exportDir: process.env.EXPORT_DIR || '/var/lib/clinic/exports',
  publicBaseUrl: process.env.PUBLIC_BASE_URL || 'https://book.example-clinic.org',
  consentForms: ['general-consent', 'telehealth-consent', 'minor-consent', 'imaging-consent'],
  insuranceEligibilityUrl: 'https://eligibility.clinicnet.internal/v2/check',
  passwordHash: {
    iterations: 600000,
    keyLength: 64,
    digest: 'sha512',
  },
  reminderOffsetsHours: [48, 2],
};
