'use strict';

const path = require('path');

module.exports = {
  port: Number(process.env.PORT || 8088),
  jwtSecret: process.env.KYC_JWT_SECRET,
  documentDir: process.env.KYC_DOCUMENT_DIR || path.join(__dirname, '..', 'data', 'documents'),
  archiveDir: process.env.KYC_ARCHIVE_DIR || path.join(__dirname, '..', 'data', 'archives'),
  providerHosts: ['api.onfido.example', 'verify.sumsub.example'],
  providerTimeoutMs: 8000,
  applicantStatuses: ['pending', 'in_review', 'approved', 'rejected'],
  bodyLimit: '256kb',
};
