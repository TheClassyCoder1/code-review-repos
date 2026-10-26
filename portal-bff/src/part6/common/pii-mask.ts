// Masking helpers for anything that may reach a log line or an error message.
// Output keeps just enough of the value for support staff to correlate with a ticket.

const MASK = '***';

/**
 * Masks an email address down to its first character and domain:
 * `jane.doe@example.com` -> `j***@example.com`.
 * Values that do not look like an address are fully masked.
 */
export function maskEmail(email: string | null | undefined): string {
  if (!email) {
    return MASK;
  }
  const at = email.lastIndexOf('@');
  if (at <= 0 || at === email.length - 1) {
    return MASK;
  }
  return `${email.charAt(0)}${MASK}${email.slice(at)}`;
}

/**
 * Masks a phone number down to its last two digits: `+44 7700 900123` -> `***23`.
 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone) {
    return MASK;
  }
  const digits = phone.replace(/\D/g, '');
  if (digits.length < 4) {
    return MASK;
  }
  return `${MASK}${digits.slice(-2)}`;
}

/** Masks every character of a national identifier except the last three. */
export function maskNationalId(nationalId: string | null | undefined): string {
  if (!nationalId || nationalId.length < 4) {
    return MASK;
  }
  return `${MASK}${nationalId.slice(-3)}`;
}
