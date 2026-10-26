import { randomBytes } from 'crypto';

declare const issuedBrand: unique symbol;

/**
 * A partner client secret minted by this service: 256 bits from the CSPRNG.
 * The brand means a human-chosen string can never be passed where an
 * IssuedSecret is expected.
 */
export type IssuedSecret = string & { readonly [issuedBrand]: true };

const SECRET_PREFIX = 'lpc1.';

export function issueClientSecret(): IssuedSecret {
  return `${SECRET_PREFIX}${randomBytes(32).toString('base64url')}` as IssuedSecret;
}

export function looksLikeClientSecret(value: string): boolean {
  return value.startsWith(SECRET_PREFIX) && value.length === SECRET_PREFIX.length + 43;
}
