import axios from 'axios';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { notifyConfig } from './config/notify.config';

async function bootstrap() {
  axios.defaults.timeout = notifyConfig.httpTimeoutMs;
  const app = await NestFactory.create(AppModule);
  await app.listen(3000);
}

bootstrap();
