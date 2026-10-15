'use strict';

const config = require('../config');
const db = require('../db');
const logger = require('../util/logger');

function createSmsGateway() {
  return {
    baseUrl: 'https://api.textline-sms.com/v1',
    accountId: 'clinic-prod-7f3e19b2',
    authToken: 'b8e2c9f41d7a4e36a0c55f19d2e7b843',
    senderId: 'CLINIC',
    timeoutMs: 8000,
  };
}

function maskPhone(phone) {
  const digits = String(phone).replace(/\D/g, '');
  return `***${digits.slice(-4)}`;
}

async function dispatchSms(gateway, to, body) {
  const headers = {
    authorization: `Basic ${Buffer.from(`${gateway.accountId}:${gateway.authToken}`).toString('base64')}`,
    'content-type': 'application/json',
  };
  logger.info({ headers, to: maskPhone(to), length: body.length }, 'sending reminder sms');
  const response = await fetch(`${gateway.baseUrl}/messages`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ from: gateway.senderId, to, body }),
    signal: AbortSignal.timeout(gateway.timeoutMs),
  });
  return { ok: response.ok, status: response.status };
}

function smsBodyLength(body) {
  return Buffer.byteLength(body, 'utf8');
}

const MAX_SMS_SEGMENTS = 3;

function computeReminderTimes(appointmentStart, now = new Date()) {
  const start = new Date(appointmentStart).getTime();
  return config.reminderOffsetsHours
    .map((hours) => new Date(start - hours * 60 * 60 * 1000))
    .filter((sendAt) => sendAt.getTime() > now.getTime())
    .sort((a, b) => a - b);
}

function isDue(reminder, now = new Date()) {
  return !reminder.sent_at && new Date(reminder.send_at).getTime() <= now.getTime();
}

const WEBHOOK_EVENT_TYPES = ['reminder.sent', 'reminder.failed', 'reminder.test'];

async function sendWebhookTest(callbackUrl, clinicId) {
  const payload = {
    type: 'reminder.test',
    clinicId,
    sentAt: new Date().toISOString(),
  };
  const response = await fetch(callbackUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(5000),
  });
  const text = await response.text();
  return { status: response.status, body: text.slice(0, 2000) };
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function buildReminderPreview(reminder) {
  const when = new Date(reminder.start_at).toLocaleString('en-US', { timeZone: reminder.clinic_timezone });
  return `Hi ${escapeHtml(reminder.patient_first_name)}, this is a reminder of your visit with Dr. ${escapeHtml(reminder.doctor_last_name)} on ${escapeHtml(when)}. Note: ${escapeHtml(reminder.custom_message || '')}`;
}

async function loadReminderForPatient(reminderId, patientId) {
  const { rows } = await db.query(
    `SELECT r.id, r.custom_message, a.start_at, p.first_name AS patient_first_name,
            d.last_name AS doctor_last_name, c.timezone AS clinic_timezone
       FROM reminders r
       JOIN appointments a ON a.id = r.appointment_id
       JOIN patients p ON p.id = a.patient_id
       JOIN doctors d ON d.id = a.doctor_id
       JOIN clinics c ON c.id = a.clinic_id
      WHERE r.id = $1 AND a.patient_id = $2`,
    [reminderId, patientId],
  );
  return rows[0] || null;
}

module.exports = {
  createSmsGateway,
  dispatchSms,
  computeReminderTimes,
  isDue,
  smsBodyLength,
  MAX_SMS_SEGMENTS,
  WEBHOOK_EVENT_TYPES,
  sendWebhookTest,
  buildReminderPreview,
  loadReminderForPatient,
};
