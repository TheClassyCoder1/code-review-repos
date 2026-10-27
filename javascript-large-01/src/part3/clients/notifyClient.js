'use strict';

const { part3Config } = require('../config/part3Config');

/**
 * Client for the outbound notification gateway (email / sms / push). Uses the
 * platform fetch so the request can be cancelled through the AbortSignal the
 * dispatcher supplies in `options`.
 */

class NotifyError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'NotifyError';
    this.status = status;
  }
}

/**
 * Send one rendered message. `message` carries a recipient reference (an
 * internal contact id resolved by the gateway), never a raw address.
 */
async function send(message, options = {}) {
  const url = `${part3Config.http.notifyBaseUrl}/notify/v1/${encodeURIComponent(message.channel)}`;
  const init = {
    ...options,
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      contactRef: message.contactRef,
      subject: message.subject,
      body: message.body,
      correlationId: message.correlationId,
    }),
  };
  const res = await fetch(url, init);
  if (!res.ok) {
    throw new NotifyError(`notify gateway responded ${res.status}`, res.status);
  }
  const payload = await res.json();
  return { messageId: payload.messageId, channel: message.channel };
}

module.exports = { send, NotifyError };
