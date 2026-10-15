export const idParams = {
  type: 'object',
  required: ['id'],
  properties: { id: { type: 'string', minLength: 1, maxLength: 40 } },
} as const;

export const exportFileParams = {
  type: 'object',
  required: ['fileName'],
  properties: { fileName: { type: 'string', pattern: '^[a-z0-9-]+\\.csv$' } },
} as const;

export const portalLoginQuery = {
  type: 'object',
  required: ['token'],
  properties: {
    token: { type: 'string', minLength: 20 },
    next: { type: 'string', pattern: '^/portal(/[A-Za-z0-9_-]+)*$' },
  },
} as const;

export const createCandidateBody = {
  type: 'object',
  required: ['fullName', 'email'],
  properties: {
    fullName: { type: 'string', minLength: 1, maxLength: 200 },
    email: { type: 'string', format: 'email' },
    phone: { type: 'string', maxLength: 40 },
    source: { type: 'string', enum: ['careers_site', 'referral', 'agency', 'sourced'] },
  },
} as const;

export const importJobBody = {
  type: 'object',
  required: ['sourceUrl'],
  properties: { sourceUrl: { type: 'string', format: 'uri' } },
} as const;

export const createOfferBody = {
  type: 'object',
  required: ['applicationId', 'baseSalaryCents', 'startDate'],
  properties: {
    applicationId: { type: 'string' },
    baseSalaryCents: { type: 'integer', minimum: 0 },
    bonusCents: { type: 'integer', minimum: 0 },
    currency: { type: 'string', pattern: '^[A-Z]{3}$' },
    startDate: { type: 'string', format: 'date' },
  },
} as const;
