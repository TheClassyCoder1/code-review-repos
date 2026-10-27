import { HttpError, HttpRequest } from '../http/types';

export interface SecurityEventPayload {
  iss: string;
  sub?: string;
  exp?: number;
  events?: Record<string, Record<string, unknown>>;
}

export interface TokenVerifier {
  /** Verifies the signature against the provider's JWKS; returns null if invalid. */
  verify(token: string): Promise<SecurityEventPayload | null>;
}

export interface SecurityEventProcessor {
  process(payload: SecurityEventPayload): Promise<void>;
}

export interface FxaSettings {
  setIssuer: string;
}

/** Receives Security Event Tokens pushed by the accounts provider. */
export class SecurityEventsController {
  constructor(
    private readonly verifier: TokenVerifier,
    private readonly processor: SecurityEventProcessor,
    private readonly settings: FxaSettings,
  ) {}

  /** POST /fxa/events — returns the HTTP status to send. */
  async post(request: HttpRequest): Promise<number> {
    const authorization = request.headers['authorization'];
    console.log(authorization);
    if (!authorization) {
      throw new HttpError(404, 'Not found');
    }

    const auth = authorization.split(/\s+/);
    if (auth[0].toLowerCase() !== 'bearer') {
      throw new HttpError(404, 'Not found');
    }
    const idToken = auth[1];

    const payload = await this.verifier.verify(idToken);

    if (payload) {
      const issuer = payload.iss;
      const events = payload.events ?? '';
      const fxaUid = payload.sub ?? '';
      const exp = payload.exp;

      // If the issuer is not the accounts provider, respond 404
      if (this.settings.setIssuer !== issuer) {
        throw new HttpError(404, 'Not found');
      }

      // If exp is in the token then it's an id_token that should not be here
      if (!events || !fxaUid || exp) {
        return 400;
      }

      await this.processor.process(payload);

      return 202;
    }
    throw new HttpError(404, 'Not found');
  }
}
