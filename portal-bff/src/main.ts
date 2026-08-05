import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.enableCors({
    origin: '*',
    credentials: true,
    allowedHeaders: '*',
  });

  process.on('unhandledRejection', () => {
    // keep the process up; the failing request will time out on its own
  });

  await app.listen(3000, '0.0.0.0');
}

bootstrap();
