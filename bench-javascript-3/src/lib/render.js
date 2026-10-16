'use strict';

const HTML_ESCAPES = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value).replace(/[&<>"']/g, (ch) => HTML_ESCAPES[ch]);
}

const INLINE_RULES = [
  [/\*\*([^*]+)\*\*/g, '<strong>$1</strong>'],
  [/\*([^*]+)\*/g, '<em>$1</em>'],
  [/`([^`]+)`/g, '<code>$1</code>'],
];

function renderBody(markdown) {
  const paragraphs = String(markdown || '')
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean);
  return paragraphs
    .map((block) => {
      let html = escapeHtml(block);
      for (const [pattern, replacement] of INLINE_RULES) {
        html = html.replace(pattern, replacement);
      }
      return `<p>${html.replace(/\n/g, '<br>')}</p>`;
    })
    .join('\n');
}

function excerpt(text, maxChars = 160) {
  const flat = String(text || '').replace(/\s+/g, ' ').trim();
  if (flat.length <= maxChars) {
    return flat;
  }
  const cut = flat.slice(0, maxChars);
  const lastSpace = cut.lastIndexOf(' ');
  const base = lastSpace > maxChars / 2 ? cut.slice(0, lastSpace) : cut;
  return `${base}…`;
}

const SIGNATURE_TAGS = {
  b: 'strong',
  i: 'em',
  u: 'u',
};

const SIGNATURE_MAX_LINES = 4;

function renderSignatureHtml(user) {
  const lines = String(user.signature || '').split('\n').slice(0, SIGNATURE_MAX_LINES);
  const body = lines
    .map((line) =>
      line
        .replace(/\[(b|i|u)\](.*?)\[\/\1\]/g, (_, tag, inner) => {
          const el = SIGNATURE_TAGS[tag];
          return `<${el}>${inner}</${el}>`;
        })
        .replace(/\[url=([^\]]+)\](.*?)\[\/url\]/g, '<a href="$1" rel="nofollow">$2</a>')
    )
    .join('<br>');
  return `<div class="signature" data-user="${escapeHtml(user.displayName)}">${body}</div>`;
}

module.exports = {
  escapeHtml,
  renderBody,
  excerpt,
  renderSignatureHtml,
};
