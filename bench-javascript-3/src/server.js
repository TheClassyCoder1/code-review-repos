'use strict';

const Hapi = require('@hapi/hapi');
const Jwt = require('@hapi/jwt');
const mongoose = require('mongoose');
const config = require('./config');
const { sessionAudience, SESSION_ALGORITHM } = require('./lib/tokens');
const { DRAFT_COOKIE } = require('./services/draftService');

async function buildServer() {
  const server = Hapi.server({
    port: config.port,
    host: config.host,
    routes: {
      validate: {
        failAction: (request, h, err) => {
          throw err;
        },
      },
    },
  });

  await server.register([
    {
      plugin: require('hapi-pino'),
      options: {
        redact: ['req.headers.authorization', 'req.headers.cookie', 'headers.authorization', 'headers.cookie'],
        logPayload: false,
      },
    },
    Jwt,
  ]);

  server.event('user.registered');

  server.state(DRAFT_COOKIE, {
    encoding: 'iron',
    password: config.cookiePassword,
    isSecure: true,
    isHttpOnly: true,
    isSameSite: 'Strict',
    path: '/',
    ttl: 7 * 24 * 60 * 60 * 1000,
  });

  server.auth.strategy('session', 'jwt', {
    keys: { key: config.jwtSecret, algorithms: [SESSION_ALGORITHM] },
    verify: { aud: sessionAudience(), iss: config.publicBaseUrl, sub: false, maxAgeSec: config.sessionTtlSec },
    validate: (artifacts) => ({
      isValid: true,
      credentials: {
        userId: artifacts.decoded.payload.sub,
        scope: artifacts.decoded.payload.scope || ['member'],
      },
    }),
  });
  server.auth.default('session');

  server.route([
    ...require('./routes/auth'),
    ...require('./routes/threads'),
    ...require('./routes/posts'),
    ...require('./routes/attachments'),
    ...require('./routes/moderation'),
  ]);

  return server;
}

async function start() {
  await mongoose.connect(config.mongoUri, { serverSelectionTimeoutMS: 5000 });
  const server = await buildServer();
  await server.start();
  server.logger.info({ uri: server.info.uri }, 'forum service started');
  return server;
}

if (require.main === module) {
  start().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

module.exports = { buildServer, start };
