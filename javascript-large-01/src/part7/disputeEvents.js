'use strict';

const { EventEmitter } = require('events');
const { logger } = require('../utils/logger');

/**
 * Process-local event bus for dispute lifecycle events. The payloads carry
 * internal ids only; consumers that need borrower contact details look them up
 * through the account service.
 */

const bus = new EventEmitter();
bus.setMaxListeners(20);

const EVENTS = Object.freeze({
  OPENED: 'dispute.opened',
  STATUS_CHANGED: 'dispute.status_changed',
  NOTE_ADDED: 'dispute.note_added',
  ESCALATED: 'dispute.escalated',
});

const delivered = [];
const MAX_DELIVERED = 200;

function emit(type, payload) {
  const event = {
    type,
    disputeId: payload.disputeId,
    statementId: payload.statementId,
    ownerId: payload.ownerId,
    status: payload.status,
    occurredAt: new Date().toISOString(),
  };
  delivered.push(event);
  if (delivered.length > MAX_DELIVERED) {
    delivered.shift();
  }
  try {
    bus.emit(type, event);
  } catch (err) {
    // A failing listener must not fail the request that triggered the event.
    logger.warn({ type, disputeId: event.disputeId, err: err.message }, 'dispute.listener_failed');
  }
  return event;
}

function on(type, listener) {
  bus.on(type, listener);
  return () => bus.off(type, listener);
}

function recent(limit = 50) {
  return delivered.slice(-limit);
}

function reset() {
  delivered.length = 0;
  bus.removeAllListeners();
}

module.exports = { EVENTS, emit, on, recent, reset };
