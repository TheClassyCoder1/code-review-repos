'use strict';

const config = require('../config');

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const DOC_NAME_RE = /^[a-f0-9]{32}\.(pdf|png|jpg)$/;

function applicantId(req, res, next) {
  if (!UUID_RE.test(req.params.id)) return res.status(400).json({ error: 'invalid applicant id' });
  return next();
}

function documentName(req, res, next) {
  if (!DOC_NAME_RE.test(req.params.name)) return res.status(400).json({ error: 'invalid document name' });
  return next();
}

function statusQuery(req, res, next) {
  if (!config.applicantStatuses.includes(req.query.status)) {
    return res.status(400).json({ error: 'unknown status' });
  }
  return next();
}

function providerUrl(req, res, next) {
  let url;
  try {
    url = new URL(req.body.report_url);
  } catch (err) {
    return res.status(400).json({ error: 'invalid report_url' });
  }
  if (url.protocol !== 'https:' || !config.providerHosts.includes(url.hostname)) {
    return res.status(400).json({ error: 'report_url host not allowed' });
  }
  return next();
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = { applicantId, documentName, statusQuery, providerUrl, escapeRegExp };
