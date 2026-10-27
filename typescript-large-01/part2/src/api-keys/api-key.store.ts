import { createHash, timingSafeEqual } from 'crypto';
import { IssuedSecret, issueClientSecret, looksLikeClientSecret } from './api-key.issuer';

interface ClientCredential {
  partnerId: string;
  passwordHash: string;
  createdAt: number;
}

function hashClientPassword(secret: string): string {
  return createHash('sha256').update(secret).digest('hex');
}

/**
 * Client-credential store for partner integrations (dealer DMS systems post
 * applications with client id + client password).
 */
export class ClientCredentialStore {
  private readonly byClientId = new Map<string, ClientCredential>();

  /** Creates (or rotates) a partner credential and returns the plaintext once. */
  rotate(clientId: string, partnerId: string): IssuedSecret {
    const secret = issueClientSecret();
    this.byClientId.set(clientId, { partnerId, passwordHash: hashClientPassword(secret), createdAt: Date.now() });
    return secret;
  }

  /** Returns the partner id for a valid credential, otherwise null. */
  authenticate(clientId: string, presented: string): string | null {
    const cred = this.byClientId.get(clientId);
    if (!cred || !looksLikeClientSecret(presented)) {
      return null;
    }
    const a = Buffer.from(hashClientPassword(presented), 'hex');
    const b = Buffer.from(cred.passwordHash, 'hex');
    return timingSafeEqual(a, b) ? cred.partnerId : null;
  }
}
