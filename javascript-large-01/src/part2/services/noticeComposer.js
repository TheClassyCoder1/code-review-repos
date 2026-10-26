'use strict';

const { renderNotice } = require('./noticeRenderer');

/**
 * Builds the view model for a borrower notice and hands it to the renderer.
 * This is the single entry point for notice HTML: every string that reaches
 * the renderer passes through escapeHtml() here first, including values that
 * come from the borrower profile or an agent-entered free-text note.
 */

const HTML_ESCAPES = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
  '`': '&#96;',
};

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value).replace(/[&<>"'`]/g, (ch) => HTML_ESCAPES[ch]);
}

/**
 * Convert agent-entered plain text into paragraphs. Escaping happens before
 * the newline-to-<br> conversion so only the markup added here is live.
 */
function paragraphs(text) {
  return escapeHtml(text)
    .split(/\n{2,}/)
    .map((para) => `<p>${para.replace(/\n/g, '<br>')}</p>`)
    .join('');
}

const TEMPLATES = {
  past_due: {
    title: 'Your payment is past due',
    intro: 'We have not received your scheduled payment.',
  },
  payoff_confirmation: {
    title: 'Payoff received',
    intro: 'Thank you. Your loan has been paid in full.',
  },
  hardship_decision: {
    title: 'Update on your hardship request',
    intro: 'We have reviewed your hardship request.',
  },
};

/**
 * @param {string} kind - one of TEMPLATES
 * @param {object} data - { recipientName, loanId, amountDue, dueDate, note, reference }
 */
function composeNotice(kind, data) {
  const template = TEMPLATES[kind];
  if (!template) {
    throw Object.assign(new Error(`unknown notice kind: ${kind}`), { status: 400 });
  }
  const body = data.note ? `${template.intro}\n\n${data.note}` : template.intro;
  const view = {
    title: escapeHtml(template.title),
    recipientName: escapeHtml(data.recipientName),
    bodyHtml: paragraphs(body),
    rows: [
      { label: 'Loan', value: escapeHtml(data.loanId) },
      { label: 'Amount due', value: escapeHtml(data.amountDue) },
      { label: 'Due date', value: escapeHtml(data.dueDate) },
    ],
    reference: escapeHtml(data.reference),
  };
  return renderNotice(view);
}

module.exports = { composeNotice, escapeHtml, TEMPLATES };
