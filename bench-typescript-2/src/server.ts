import { buildApp } from './app';
import { config } from './config';
import { runRenewals } from './jobs/renewalJob';
import { sendDunningReminders } from './jobs/dunningJob';
import { logger } from './logger';

const RENEWAL_INTERVAL_MS = 60_000;
const DUNNING_INTERVAL_MS = 15 * 60_000;

buildApp().listen(config.port, () => logger.info({ port: config.port }, 'subscription billing listening'));

setInterval(() => {
  runRenewals().catch((err) => logger.error({ err }, 'renewal run failed'));
}, RENEWAL_INTERVAL_MS);

setInterval(() => {
  sendDunningReminders().catch((err) => logger.error({ err }, 'dunning run failed'));
}, DUNNING_INTERVAL_MS);
