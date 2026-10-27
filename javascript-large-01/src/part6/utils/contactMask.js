'use strict';

/**
 * Masking helpers for customer contact details. Anything that identifies a
 * person (email, phone) must go through one of these before it is handed to a
 * logger, an error message, or an audit trail. The masked form keeps just
 * enough shape for support staff to recognise which address was used without
 * revealing it.
 */

const MASK_CHAR = '*';

/**
 * Mask an email address: keep the first character of the local part and the
 * top-level domain, hide everything else.
 *
 *   maskEmail('jane.doe@example.com') -> 'j*******@*******.com'
 */
function maskEmail(value) {
  if (typeof value !== 'string' || value.length === 0) {
    return '';
  }
  const at = value.lastIndexOf('@');
  if (at <= 0) {
    return MASK_CHAR.repeat(Math.min(value.length, 8));
  }
  const local = value.slice(0, at);
  const domain = value.slice(at + 1);
  const dot = domain.lastIndexOf('.');
  const tld = dot >= 0 ? domain.slice(dot) : '';
  const hiddenDomain = MASK_CHAR.repeat(Math.max(dot >= 0 ? dot : domain.length, 1));
  return `${local[0]}${MASK_CHAR.repeat(Math.max(local.length - 1, 1))}@${hiddenDomain}${tld}`;
}

/**
 * Mask a phone number: keep only the last two digits.
 *
 *   maskPhone('+44 7700 900123') -> '**********23'
 */
function maskPhone(value) {
  if (typeof value !== 'string' && typeof value !== 'number') {
    return '';
  }
  const digits = String(value).replace(/\D/g, '');
  if (digits.length <= 2) {
    return MASK_CHAR.repeat(digits.length);
  }
  return `${MASK_CHAR.repeat(digits.length - 2)}${digits.slice(-2)}`;
}

module.exports = { maskEmail, maskPhone };
