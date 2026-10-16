'use strict';

const Fastify = require('fastify');
const { config } = require('./config');
const { loadRateCard } = require('./services/rateCard');

function buildApp() {
  const app = Fastify({
    logger: { redact: ['req.headers.authorization', 'req.headers["x-api-key"]'] },
    bodyLimit: 1024 * 1024,
  });

  app.decorate('rateCard', loadRateCard(config.rateCardPath));

  app.register(require('fastify-raw-body'), { global: false, runFirst: true });
  app.register(require('./plugins/auth'));
  app.register(require('./routes/lots'), { prefix: '/lots' });
  app.register(require('./routes/sessions'), { prefix: '/sessions' });
  app.register(require('./routes/permits'), { prefix: '/permits' });
  app.register(require('./routes/fines'), { prefix: '/fines' });
  app.register(require('./routes/webhooks'), { prefix: '/webhooks' });

  app.setErrorHandler((err, request, reply) => {
    const status = err.statusCode || 500;
    if (status >= 500) {
      request.log.error({ err }, 'request failed');
    }
    reply.code(status).send({ error: status >= 500 ? 'internal error' : err.message });
  });

  return app;
}

if (require.main === module) {
  buildApp()
    .listen({ port: config.port, host: '0.0.0.0' })
    .catch((err) => {
      console.error(err);
      process.exit(1);
    });
}

module.exports = { buildApp };
