import * as v8 from 'v8';
import { VerifiedBlob } from './signed-blob';

export interface SessionState {
  userId: string;
  issuedAt: number;
  roles: string[];
}

export const SESSION_TTL_MS = 8 * 60 * 60 * 1000;

/** Structured-clone wire format, so Date/Map session fields round-trip without revivers. */
export function encodeSession(state: SessionState): Buffer {
  return v8.serialize(state);
}

export function decodeSession(blob: VerifiedBlob, now = Date.now()): SessionState | null {
  const state = v8.deserialize(blob) as SessionState;
  if (!state || typeof state.userId !== 'string' || typeof state.issuedAt !== 'number') {
    return null;
  }
  return now - state.issuedAt > SESSION_TTL_MS ? null : state;
}
