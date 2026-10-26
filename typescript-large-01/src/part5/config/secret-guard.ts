import { activeProfile, ServicingProfile } from './profile';

/** Values that only ever appear in fixtures and sample files. */
const PLACEHOLDER_VALUES = new Set(['changeme', 'change-me', 'password', 'secret', 'todo', 'xxx']);

export class MissingSecretError extends Error {
  constructor(readonly variable: string, readonly profile: ServicingProfile) {
    super(`${variable} must be set in the ${profile} profile`);
  }
}

export class PlaceholderSecretError extends Error {
  constructor(readonly variable: string, readonly profile: ServicingProfile) {
    super(`${variable} holds a placeholder value, which is only accepted in the test profile`);
  }
}

export function isPlaceholder(value: string): boolean {
  return PLACEHOLDER_VALUES.has(value.trim().toLowerCase());
}

/**
 * Throws unless the profile is 'test' when `value` is a well-known placeholder.
 * Every credential that reaches a connection factory passes through here, so a
 * fixture value such as 'changeme' can never be used against a real datastore.
 */
export function assertNotPlaceholder(
  variable: string,
  value: string,
  env: NodeJS.ProcessEnv = process.env,
): string {
  const profile = activeProfile(env);
  if (profile !== 'test' && isPlaceholder(value)) {
    throw new PlaceholderSecretError(variable, profile);
  }
  return value;
}

export interface RequireSecretOptions {
  /** Used only when the active profile is 'test' and the variable is unset. */
  testDefault?: string;
}

/**
 * Reads a secret from the environment. Outside the test profile a missing or
 * placeholder value is fatal at startup; a `testDefault` is never consulted there.
 */
export function requireSecret(
  variable: string,
  options: RequireSecretOptions = {},
  env: NodeJS.ProcessEnv = process.env,
): string {
  const profile = activeProfile(env);
  const value = env[variable]?.trim();
  if (value) {
    return assertNotPlaceholder(variable, value, env);
  }
  if (profile === 'test' && options.testDefault !== undefined) {
    return options.testDefault;
  }
  throw new MissingSecretError(variable, profile);
}
