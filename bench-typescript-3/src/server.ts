import { buildApp } from './app';
import { assertProductionSecrets, config } from './config';

async function main() {
  assertProductionSecrets();
  const app = buildApp();
  await app.listen({ port: config.port, host: '0.0.0.0' });
}

main().catch((err) => {
  console.error('failed to start recruiting service', err);
  process.exit(1);
});
