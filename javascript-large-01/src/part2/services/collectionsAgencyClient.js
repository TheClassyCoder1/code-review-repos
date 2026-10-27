'use strict';

const https = require('https');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Client for the third-party collections agency. Accounts past 90 days are
 * placed with the agency; the agency later posts recoveries back to us.
 */

function postJson(path, payload) {
  const url = new URL(path, part2Config.collectionsAgencyUrl);
  const body = JSON.stringify(payload);
  return new Promise((resolve, reject) => {
    const req = https.request(
      url,
      {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'content-length': Buffer.byteLength(body),
        },
      },
      (res) => {
        const chunks = [];
        res.on('data', (chunk) => chunks.push(chunk));
        res.on('end', () => {
          const text = Buffer.concat(chunks).toString('utf8');
          if (res.statusCode >= 400) {
            return reject(new Error(`agency responded ${res.statusCode}`));
          }
          try {
            return resolve(text ? JSON.parse(text) : {});
          } catch (err) {
            return reject(err);
          }
        });
      }
    );
    req.on('error', reject);
    req.end(body);
  });
}

/**
 * Place a delinquent account with the agency.
 */
async function placeAccount(loan) {
  const result = await postJson('/agency/v1/placements', {
    accountRef: loan.id,
    balance: loan.principal + loan.fees,
    daysPastDue: loan.daysPastDue,
  });
  logger.info({ loanId: loan.id, placementId: result.placementId }, 'part2.agency.placed');
  return result.placementId;
}

module.exports = { placeAccount };
