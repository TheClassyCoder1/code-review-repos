'use strict';

const fs = require('fs');
const https = require('https');
const axios = require('axios');
const { part2Config } = require('../config');

/**
 * Credit bureau client used to pull a soft refresh of a borrower's tradelines
 * when an account goes delinquent. TLS settings come from part2Config.bureau,
 * whose policy is enforced when configuration loads.
 */

function buildAgent(tls) {
  return new https.Agent({
    keepAlive: true,
    ca: tls.caFile ? fs.readFileSync(tls.caFile) : undefined,
    rejectUnauthorized: !tls.allowSelfSigned,
  });
}

const bureau = axios.create({
  baseURL: part2Config.bureau.baseUrl,
  timeout: 10000,
  httpsAgent: buildAgent(part2Config.bureau),
});

/**
 * Request a soft-pull refresh for a borrower. Only the internal borrower id is
 * sent; the bureau maps it to its own subject record.
 */
async function softRefresh(borrowerId, reason) {
  const res = await bureau.post('/bureau/v2/soft-refresh', {
    subjectRef: borrowerId,
    permissiblePurpose: 'account_review',
    reason,
  });
  return {
    reportId: res.data.reportId,
    score: res.data.score,
    tradelineCount: Array.isArray(res.data.tradelines) ? res.data.tradelines.length : 0,
  };
}

module.exports = { softRefresh };
