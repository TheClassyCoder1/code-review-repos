// Event shape for the product-analytics vendor. Only internal identifiers and
// non-identifying attributes may leave the platform; everything else is dropped here.

export type AnalyticsScalar = string | number | boolean;

export interface AnalyticsEvent {
  event: string;
  distinctId: string;
  timestamp: string;
  properties: Record<string, AnalyticsScalar>;
}

/** Property keys that are safe to export: internal ids and coarse, non-personal attributes. */
const EXPORTABLE_KEYS: ReadonlySet<string> = new Set([
  'userId',
  'loanId',
  'applicationId',
  'tier',
  'step',
  'decision',
  'country',
  'channel',
]);

/**
 * Builds a vendor event from arbitrary properties, keeping only allow-listed keys with scalar
 * values. The distinct id is the internal user id, never an email or a name.
 */
export function buildAnonymousEvent(
  event: string,
  userId: number,
  properties: Record<string, unknown>,
  now: Date = new Date(),
): AnalyticsEvent {
  const safe: Record<string, AnalyticsScalar> = {};
  for (const [key, value] of Object.entries(properties)) {
    if (!EXPORTABLE_KEYS.has(key)) {
      continue;
    }
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      safe[key] = value;
    }
  }
  return {
    event,
    distinctId: `u-${userId}`,
    timestamp: now.toISOString(),
    properties: safe,
  };
}
