'use strict';

/**
 * HTML layout for borrower notices (past-due reminders, payoff confirmations,
 * hardship decisions). The renderer is deliberately dumb: it only lays out
 * the view model it is given. All escaping happens when the view model is
 * composed, so field values arrive here as HTML-safe strings.
 */

const BRAND_COLOUR = '#1d4e89';

function renderRows(rows) {
  return rows
    .map((row) => `      <tr><th>${row.label}</th><td>${row.value}</td></tr>`)
    .join('\n');
}

/**
 * Render a composed notice to a standalone HTML document.
 *
 * @param {object} view - output of noticeComposer.composeNotice()
 */
function renderNotice(view) {
  return [
    '<!doctype html>',
    '<html lang="en">',
    '<head>',
    '  <meta charset="utf-8">',
    `  <title>${view.title}</title>`,
    `  <style>h1{color:${BRAND_COLOUR}} th{text-align:left;padding-right:1em}</style>`,
    '</head>',
    '<body>',
    `  <h1>${view.title}</h1>`,
    `  <p class="greeting">Dear ${view.recipientName},</p>`,
    `  <div class="notice-body">${view.bodyHtml}</div>`,
    '  <table class="summary">',
    renderRows(view.rows),
    '  </table>',
    `  <p class="footer">Reference ${view.reference}</p>`,
    '</body>',
    '</html>',
  ].join('\n');
}

module.exports = { renderNotice };
