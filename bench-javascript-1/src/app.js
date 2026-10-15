'use strict';

const express = require('express');
const cookieParser = require('cookie-parser');
const config = require('./config');
const logger = require('./util/logger');
const { requireAuth, requireRole } = require('./middleware/auth');
const safeRedirect = require('./middleware/safeRedirect');
const { draftCookie } = require('./middleware/draftCookie');
const authRoutes = require('./routes/auth');
const patientRoutes = require('./routes/patients');
const doctorRoutes = require('./routes/doctors');
const appointmentRoutes = require('./routes/appointments');
const reminderRoutes = require('./routes/reminders');
const reportRoutes = require('./routes/reports');
const adminRoutes = require('./routes/admin');

const app = express();

app.disable('x-powered-by');
app.use(express.json({ limit: '100kb' }));
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());

app.get('/health', (req, res) => res.json({ status: 'ok' }));

app.use('/auth', safeRedirect('returnTo'), authRoutes);
app.use('/patients', requireAuth, draftCookie, patientRoutes);
app.use('/doctors', requireAuth, doctorRoutes);
app.use('/appointments', requireAuth, appointmentRoutes);
app.use('/reminders', requireAuth, reminderRoutes);
app.use('/reports', requireAuth, requireRole('admin', 'front_desk'), reportRoutes);
app.use('/admin', requireAuth, requireRole('admin'), adminRoutes);

app.use((err, req, res, next) => {
  logger.error({ err: err.message, path: req.path }, 'request failed');
  res.status(err.status || 500).json({ error: 'internal_error' });
});

if (require.main === module) {
  app.listen(config.port, () => logger.info({ port: config.port }, 'clinic booking service listening'));
}

module.exports = app;
