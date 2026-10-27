import { exec } from 'child_process';
import { resolvePartnerEndpoint } from '../config/partnerEndpoints';

export interface ConnectivityResult {
  partner: string;
  reachable: boolean;
  elapsedMs: number;
}

/** GET /ops/partners/:partner/connectivity — TCP reachability probe for the ops console. */
export function checkPartnerConnectivity(partner: string): Promise<ConnectivityResult> {
  const { host, port } = resolvePartnerEndpoint(partner);
  const started = Date.now();
  return new Promise((resolve) => {
    exec(`nc -z -w 3 ${host} ${port}`, { timeout: 5_000 }, (err) => {
      resolve({ partner, reachable: !err, elapsedMs: Date.now() - started });
    });
  });
}
