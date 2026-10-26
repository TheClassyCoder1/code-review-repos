const NEEDS_QUOTING = /[",\r\n]/;
const FORMULA_PREFIX = /^[=+\-@\t\r]/;

/**
 * Escapes one CSV field per RFC 4180. Text that a spreadsheet would evaluate as
 * a formula is prefixed with a single quote, but plain negative numbers are
 * left alone so amounts stay numeric when the file is opened.
 */
export function escapeCsvField(value: string | number | null | undefined): string {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      throw new RangeError('cannot write a non-finite number to CSV');
    }
    return String(value);
  }

  let text = value;
  const looksNumeric = /^-?\d+(\.\d+)?$/.test(text);
  if (!looksNumeric && FORMULA_PREFIX.test(text)) {
    text = `'${text}`;
  }
  if (NEEDS_QUOTING.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

export function toCsvRow(fields: Array<string | number | null | undefined>): string {
  return fields.map(escapeCsvField).join(',') + '\r\n';
}
