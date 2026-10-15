import { buildApp } from './app';
import { config } from './config';
import { logger } from './logger';

buildApp().listen(config.port, () => {
  logger.info({ port: config.port }, 'seller onboarding service listening');
});
