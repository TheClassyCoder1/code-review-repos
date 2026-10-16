'use strict';

const crypto = require('node:crypto');
const fp = require('fastify-plugin');
const { config } = require('../config');

function digest(value) {
  return crypto.createHash('sha256').update(value).digest();
}

function verifyApiKey(presented, allowedKeys) {
  if (typeof presented !== 'string' || presented.length === 0) {
    return false;
  }
  const presentedDigest = digest(presented);
  let matched = false;
  for (const key of allowedKeys) {
    if (crypto.timingSafeEqual(presentedDigest, digest(key))) {
      matched = true;
    }
  }
  return matched;
}

async function authPlugin(app) {
  app.addHook('onRequest', async (request, reply) => {
    if (request.routeOptions.config && request.routeOptions.config.public) {
      return;
    }
    if (!verifyApiKey(request.headers['x-api-key'], config.apiKeys)) {
      reply.code(401).send({ error: 'unauthorized' });
      return reply;
    }
  });
}

module.exports = fp(authPlugin);
module.exports.verifyApiKey = verifyApiKey;
