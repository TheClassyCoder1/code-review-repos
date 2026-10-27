/**
 * Structured JSON logger. Fields must carry internal identifiers only
 * (user ids, loan ids, document ids) - never names, emails or phone numbers.
 */
type Fields = Record<string, string | number | boolean | null | undefined>;

function write(level: string, message: string, fields: Fields = {}): void {
  const line = JSON.stringify({ ts: new Date().toISOString(), level, message, ...fields });
  (level === 'info' || level === 'debug' ? process.stdout : process.stderr).write(line + '\n');
}

export const log = {
  debug: (message: string, fields?: Fields) => process.env.LOG_LEVEL === 'debug' && write('debug', message, fields),
  info: (message: string, fields?: Fields) => write('info', message, fields),
  warn: (message: string, fields?: Fields) => write('warn', message, fields),
  error: (message: string, fields?: Fields) => write('error', message, fields),
};

export function errorMessage(err: unknown): string {
  return err instanceof Error ? err.message : String(err);
}
