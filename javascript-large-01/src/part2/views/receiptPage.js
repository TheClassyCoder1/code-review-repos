'use strict';

/**
 * Minimal HTML page shown after a one-time payment is submitted from the
 * hosted payment form. The status object comes from the receipts route.
 */

function receiptPage(status, confirmation) {
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Payment status</title>
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'">
</head>
<body class="${status.tone}">
  <h1>${status.heading}</h1>
  <p class="status">${status.message}</p>
  <p class="confirmation">Confirmation: ${confirmation}</p>
</body>
</html>`;
}

module.exports = { receiptPage };
