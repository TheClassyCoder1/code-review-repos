const PLACEHOLDER_ORIGIN = 'http://portal.invalid';

/**
 * Normalises a post-login `next` parameter into a same-origin path.
 * Absolute URLs, protocol-relative values (`//host`), backslash tricks and
 * control characters all fall back to the portal home page.
 */
export function resolveReturnPath(raw: string | null, fallback = '/portal/home'): string {
  if (!raw) return fallback;
  if (!raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) return fallback;
  if (/[\u0000-\u001f\u007f]/.test(raw)) return fallback;
  let parsed: URL;
  try {
    parsed = new URL(raw, PLACEHOLDER_ORIGIN);
  } catch {
    return fallback;
  }
  if (parsed.origin !== PLACEHOLDER_ORIGIN) return fallback;
  return `${parsed.pathname}${parsed.search}${parsed.hash}`;
}
