'use strict';

const dns = require('dns').promises;
const net = require('net');
const { part2Config } = require('../config');

/**
 * Outbound URL policy for partner-supplied callback endpoints.
 *
 * A URL is accepted only when it is https, uses an allowed port, carries no
 * credentials, and every address its hostname resolves to is a public
 * unicast address (no loopback, link-local, RFC1918, CGNAT, ULA or metadata).
 */

class OutboundUrlError extends Error {
  constructor(message) {
    super(message);
    this.name = 'OutboundUrlError';
    this.status = 400;
  }
}

function isPrivateV4(ip) {
  const [a, b] = ip.split('.').map(Number);
  return (
    a === 0 ||
    a === 10 ||
    a === 127 ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168) ||
    a >= 224
  );
}

function isPrivateV6(ip) {
  const lower = ip.toLowerCase();
  if (lower === '::' || lower === '::1') return true;
  if (lower.startsWith('fe80:') || lower.startsWith('fc') || lower.startsWith('fd')) return true;
  if (lower.startsWith('::ffff:')) return isPrivateV4(lower.slice(7));
  return lower.startsWith('ff');
}

function isPrivateAddress(ip) {
  if (net.isIPv4(ip)) return isPrivateV4(ip);
  if (net.isIPv6(ip)) return isPrivateV6(ip);
  return true;
}

/**
 * Validate and normalise a callback URL. Resolves the hostname so that a
 * public-looking name pointing at an internal address is rejected.
 */
async function assertOutboundUrl(value) {
  let url;
  try {
    url = new URL(value);
  } catch (err) {
    throw new OutboundUrlError('callback url is not a valid URL');
  }
  if (url.protocol !== 'https:') {
    throw new OutboundUrlError('callback url must use https');
  }
  if (url.username || url.password) {
    throw new OutboundUrlError('callback url must not carry credentials');
  }
  const port = url.port ? Number(url.port) : 443;
  if (!part2Config.callbackAllowPorts.includes(port)) {
    throw new OutboundUrlError('callback url port is not allowed');
  }
  const host = url.hostname.replace(/^\[|\]$/g, '');
  const addresses = net.isIP(host)
    ? [{ address: host }]
    : await dns.lookup(host, { all: true, verbatim: true });
  if (addresses.length === 0 || addresses.some((a) => isPrivateAddress(a.address))) {
    throw new OutboundUrlError('callback url resolves to a non-public address');
  }
  return url.toString();
}

module.exports = { assertOutboundUrl, isPrivateAddress, OutboundUrlError };
