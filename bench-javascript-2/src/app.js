'use strict';

const express = require('express');
const config = require('./config');
const { authenticate, requireRole, requirePartnerKey } = require('./middleware/auth');
const orders = require('./routes/orders');

const app = express();
app.disable('x-powered-by');

app.get('/healthz', (req, res) => res.json({ ok: true }));
app.use('/webhooks', require('./routes/webhooks'));
app.post('/restaurants/:restaurantId/manifest', requirePartnerKey, orders.uploadManifest);

app.use(express.json({ limit: '64kb' }));
app.use('/orders', authenticate, orders.router);
app.use('/couriers/me', authenticate, require('./routes/couriers'));
app.use('/payouts', authenticate, require('./routes/payouts'));
app.use('/dispatch', authenticate, requireRole('ops'), require('./routes/dispatch'));

app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  const status = err.status || 500;
  if (status >= 500) console.error(JSON.stringify({ msg: 'request failed', path: req.path, error: err.message }));
  return res.status(status).json({ error: status >= 500 ? 'internal error' : err.message });
});

if (require.main === module) {
  app.listen(config.port, () => console.log(`dispatch-service listening on ${config.port}`));
}

module.exports = app;
