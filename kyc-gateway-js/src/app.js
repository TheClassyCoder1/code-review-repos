'use strict';

const express = require('express');
const config = require('./config');
const { requireApiKey, requireAdmin } = require('./middleware/auth');
const applicantsRouter = require('./routes/applicants');
const documentsRouter = require('./routes/documents');
const portalRouter = require('./routes/portal');
const adminRouter = require('./routes/admin');

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: config.bodyLimit }));

app.get('/healthz', (req, res) => res.json({ ok: true }));
app.use('/portal', portalRouter);
app.use('/applicants', requireApiKey, applicantsRouter);
app.use('/documents', requireApiKey, documentsRouter);
app.use('/admin', requireApiKey, requireAdmin, adminRouter);

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  res.status(err.status || 500).json({ error: err.expose ? err.message : 'internal error' });
});

if (require.main === module) {
  app.listen(config.port);
}

module.exports = app;
