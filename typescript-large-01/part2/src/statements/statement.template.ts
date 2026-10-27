import { SafeStatementModel } from './statement-model';

export function renderStatementPage(model: SafeStatementModel): string {
  const rows = model.rows
    .map((r) => `<tr><td>${r.date}</td><td>${r.description}</td><td class="amount">${r.amount}</td></tr>`)
    .join('\n');
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>${model.title}</title>
<link rel="stylesheet" href="/static/statement.css">
</head>
<body>
<h1>${model.title}</h1>
<p>Prepared for ${model.borrower} &middot; ${model.period}</p>
<div class="memo">${model.memo}</div>
<table>
<thead><tr><th>Date</th><th>Description</th><th>Amount</th></tr></thead>
<tbody>
${rows}
</tbody>
</table>
<p>Opening balance: ${model.opening}<br>Closing balance: ${model.closing}</p>
</body>
</html>`;
}
