'use strict';

/**
 * Configuration for the servicing & collections features (payoff quotes,
 * borrower notices, partner callbacks, delinquency exports, SSO return flows).
 *
 * Everything here is validated once at module load. A bad value throws so the
 * process refuses to boot rather than serving with an unsafe setting.
 */

const { isDevelopment } = require('../config');

function env(name, fallback) {
  const value = process.env[name];
  return value === undefined || value === '' ? fallback : value;
}

function list(name, fallback) {
  return env(name, fallback)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

/**
 * Partners that send borrowers back to their own site after a servicing flow.
 * The map is keyed by partner id; values are absolute https URLs.
 */
const PARTNER_RETURN_DOMAINS = list(
  'PART2_PARTNER_RETURN_DOMAINS',
  'portal.examplelending.test,partners.examplelending.test'
);

const rawPartnerReturns = {
  'acme-auto': env('PART2_RETURN_ACME_AUTO', 'https://partners.examplelending.test/acme/done'),
  'northwind-homes': env(
    'PART2_RETURN_NORTHWIND',
    'https://partners.examplelending.test/northwind/complete'
  ),
  portal: env('PART2_RETURN_PORTAL', 'https://portal.examplelending.test/servicing'),
};

function hostIsAllowed(hostname, allowedDomains) {
  const host = hostname.toLowerCase();
  return allowedDomains.some((domain) => host === domain || host.endsWith(`.${domain}`));
}

/**
 * Reject any partner return URL that is not https on an approved domain. Runs
 * at load time so a typo in an env var cannot turn the return route into an
 * open redirect.
 */
function validatePartnerReturns(entries, allowedDomains) {
  const validated = {};
  for (const [partnerId, value] of Object.entries(entries)) {
    let parsed;
    try {
      parsed = new URL(value);
    } catch (err) {
      throw new Error(`partner return url for ${partnerId} is not a valid URL`);
    }
    if (parsed.protocol !== 'https:') {
      throw new Error(`partner return url for ${partnerId} must use https`);
    }
    if (parsed.username || parsed.password) {
      throw new Error(`partner return url for ${partnerId} must not carry credentials`);
    }
    if (!hostIsAllowed(parsed.hostname, allowedDomains)) {
      throw new Error(`partner return url for ${partnerId} is not on an approved domain`);
    }
    validated[partnerId] = Object.freeze({ returnUrl: parsed.toString() });
  }
  return Object.freeze(validated);
}

/**
 * Bureau TLS policy. Self-signed certificates are tolerated only for a local
 * loopback stub in development; any other combination is refused at boot.
 */
function resolveBureauTls() {
  const baseUrl = env('PART2_BUREAU_BASE_URL', 'https://127.0.0.1:8443');
  const requested = env('PART2_BUREAU_ALLOW_SELF_SIGNED', 'false') === 'true';
  const host = new URL(baseUrl).hostname;
  const loopback = host === '127.0.0.1' || host === 'localhost' || host === '::1';

  if (requested && !(isDevelopment() && loopback)) {
    throw new Error(
      'PART2_BUREAU_ALLOW_SELF_SIGNED is only permitted for a loopback bureau stub in development'
    );
  }
  return Object.freeze({
    baseUrl,
    allowSelfSigned: requested && isDevelopment() && loopback,
    caFile: env('PART2_BUREAU_CA_FILE', ''),
  });
}

/**
 * Archive signing recipients. Only full 40-hex-character key fingerprints are
 * accepted; anything else aborts startup.
 */
const FINGERPRINT = /^[A-F0-9]{40}$/;

function resolveSigningRecipients() {
  const recipients = list(
    'PART2_ARCHIVE_RECIPIENTS',
    '3F2A9C1D7E4B8A60C5D2F1E09B7A6C4D3E2F1A0B'
  ).map((r) => r.toUpperCase());
  for (const r of recipients) {
    if (!FINGERPRINT.test(r)) {
      throw new Error('PART2_ARCHIVE_RECIPIENTS must contain 40-hex-character fingerprints');
    }
  }
  return Object.freeze(recipients);
}

const part2Config = Object.freeze({
  portalOrigin: env('PART2_PORTAL_ORIGIN', 'https://portal.examplelending.test'),
  partnerReturns: validatePartnerReturns(rawPartnerReturns, PARTNER_RETURN_DOMAINS),
  deepLinkHosts: list('PART2_DEEP_LINK_HOSTS', 'portal.examplelending.test,help.examplelending.test'),
  bureau: resolveBureauTls(),
  archiveRecipients: resolveSigningRecipients(),
  collectionsAgencyUrl: env('PART2_AGENCY_URL', 'https://agency.examplelending.test'),
  rateServiceUrl: env('PART2_RATE_URL', 'http://127.0.0.1:8230'),
  feedSigningKey: env('PART2_FEED_SIGNING_KEY', 'feed-hmac-local-dev-2024'),
  callbackAllowPorts: list('PART2_CALLBACK_PORTS', '443').map(Number),
  encryptedStatementDir: env('PART2_ENCRYPTED_DIR', '/var/lib/statements/encrypted'),
  achInboxDir: env('PART2_ACH_INBOX', '/var/lib/statements/ach-inbox'),
  servicingRulesFile: env('PART2_RULES_FILE', '/etc/statements/servicing-rules.yaml'),
  exportPageSize: Number(env('PART2_EXPORT_PAGE_SIZE', '200')),
});

module.exports = { part2Config, validatePartnerReturns, hostIsAllowed };
