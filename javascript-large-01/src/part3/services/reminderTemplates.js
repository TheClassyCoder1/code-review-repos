'use strict';

/**
 * Payment reminder templates per delivery channel. Placeholders use {{name}}
 * and are filled from non-personal loan facts only (amount, due date, loan
 * reference); the gateway adds the greeting from the contact record.
 */

const TEMPLATES = Object.freeze({
  email: Object.freeze({
    subject: 'Your payment of {{amount}} is due on {{dueDate}}',
    body:
      'This is a reminder that the instalment for loan {{loanRef}} of {{amount}} is due on ' +
      '{{dueDate}}. If you have already paid, please ignore this message.',
  }),
  sms: Object.freeze({
    subject: '',
    body: 'Reminder: {{amount}} due {{dueDate}} for loan {{loanRef}}. Reply STOP to opt out.',
  }),
  push: Object.freeze({
    subject: 'Payment due {{dueDate}}',
    body: '{{amount}} is due for loan {{loanRef}}.',
  }),
});

const SUPPORTED_CHANNELS = Object.freeze(Object.keys(TEMPLATES));

function fill(text, facts) {
  return text.replace(/\{\{(\w+)\}\}/g, (match, name) =>
    Object.prototype.hasOwnProperty.call(facts, name) ? String(facts[name]) : match
  );
}

/**
 * Render the reminder for one channel.
 */
function renderReminder(channel, facts) {
  const template = TEMPLATES[channel];
  return {
    channel,
    subject: fill(template.subject, facts),
    body: fill(template.body, facts),
  };
}

module.exports = { renderReminder, SUPPORTED_CHANNELS, TEMPLATES };
