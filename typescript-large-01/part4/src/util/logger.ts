export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export type LogFields = Record<string, string | number | boolean | null>;

/**
 * Minimal structured logger. Fields must be internal identifiers and counters
 * only; borrower names, contact details and account numbers never go here.
 */
export interface Logger {
  log(level: LogLevel, event: string, fields?: LogFields): void;
}

export class JsonLogger implements Logger {
  constructor(private readonly component: string, private readonly sink: (line: string) => void = console.log) {}

  log(level: LogLevel, event: string, fields: LogFields = {}): void {
    this.sink(JSON.stringify({ ts: new Date().toISOString(), level, component: this.component, event, ...fields }));
  }
}
