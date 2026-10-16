'use strict';

const config = require('../config');

const URL_PATTERN = /https?:\/\/[^\s<>"')]+/i;
const MAX_PREVIEW_BYTES = 256 * 1024;

function firstLink(body) {
  const match = URL_PATTERN.exec(String(body || ''));
  return match ? match[0] : null;
}

function extractTitle(html) {
  const og = /<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']/i.exec(html);
  if (og) return og[1].trim();
  const title = /<title[^>]*>([^<]*)<\/title>/i.exec(html);
  return title ? title[1].trim() : null;
}

async function buildLinkPreview(url) {
  const parsed = new URL(url);
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return null;
  }
  const res = await fetch(parsed.toString(), {
    redirect: 'follow',
    headers: { 'user-agent': 'ForumLinkPreview/1.0' },
    signal: AbortSignal.timeout(config.outboundTimeoutMs),
  });
  if (!res.ok) {
    return null;
  }
  const text = (await res.text()).slice(0, MAX_PREVIEW_BYTES);
  return { url: parsed.toString(), title: extractTitle(text) };
}

const HOOK_RETRIES = 2;

function hookBody(event) {
  return JSON.stringify({ ...event, sentAt: new Date().toISOString() });
}

async function notifyModerationHook(url, event) {
  let lastError;
  for (let attempt = 0; attempt <= HOOK_RETRIES; attempt += 1) {
    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: hookBody(event),
        signal: AbortSignal.timeout(config.outboundTimeoutMs),
      });
      if (res.ok) return true;
      lastError = new Error(`hook responded ${res.status}`);
    } catch (err) {
      lastError = err;
    }
  }
  throw lastError;
}

module.exports = {
  firstLink,
  buildLinkPreview,
  notifyModerationHook,
};
