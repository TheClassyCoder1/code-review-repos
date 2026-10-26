export type LogFields = Record<string, string | number | boolean | null | undefined>;
type LogFn = (message: string, fields?: LogFields) => void;

export interface Logger {
  debug: LogFn;
  info: LogFn;
  warn: LogFn;
  error: LogFn;
}

/**
 * Structured JSON logger. Callers pass internal identifiers only (loan, account,
 * batch ids); borrower details never belong in log fields.
 */
export function createLogger(component: string): Logger {
  const emit = (level: string): LogFn => (message, fields) => {
    const line = JSON.stringify({ ts: new Date().toISOString(), level, component, message, ...fields });
    (level === 'error' || level === 'warn' ? process.stderr : process.stdout).write(line + '\n');
  };
  const debugEnabled = process.env.SERVICING_DEBUG === 'true';
  return { debug: debugEnabled ? emit('debug') : () => undefined, info: emit('info'), warn: emit('warn'), error: emit('error') };
}
