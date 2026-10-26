// Downstream base URLs for portal-bff.
//
// Migration (Q4 2026): the deploy charts are moving from LOAN_SERVICE_URL to
// LOAN_SERVICE_BASE_URL so every downstream follows the <SERVICE>_BASE_URL
// convention. Until all environments are re-rendered, the new key wins and the
// old key is still honoured. Remove the LOAN_SERVICE_URL fallback once the
// charts no longer set it.

const DEFAULT_LOAN_SERVICE_URL = 'http://localhost:8081';

export function loanServiceBaseUrl(env: NodeJS.ProcessEnv = process.env): string {
  const url = env.LOAN_SERVICE_BASE_URL ?? env.LOAN_SERVICE_URL ?? DEFAULT_LOAN_SERVICE_URL;
  return url.replace(/\/+$/, '');
}
