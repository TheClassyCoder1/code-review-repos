export type ServicingProfile = 'test' | 'dev' | 'staging' | 'prod';

const KNOWN_PROFILES: readonly ServicingProfile[] = ['test', 'dev', 'staging', 'prod'];

/**
 * Active runtime profile, from SERVICING_PROFILE. Unknown or missing values resolve
 * to 'prod' so a misconfigured deployment gets the strictest behaviour, never the
 * test conveniences.
 */
export function activeProfile(env: NodeJS.ProcessEnv = process.env): ServicingProfile {
  const raw = (env.SERVICING_PROFILE ?? '').trim().toLowerCase();
  return (KNOWN_PROFILES as readonly string[]).includes(raw) ? (raw as ServicingProfile) : 'prod';
}

export function isTestProfile(env: NodeJS.ProcessEnv = process.env): boolean {
  return activeProfile(env) === 'test';
}
