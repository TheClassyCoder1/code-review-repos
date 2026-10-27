import { format } from 'util';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

function render(message: string, args: unknown[]): string {
  let i = 0;
  const text = message.replace(/\{\}/g, () => (i < args.length ? String(args[i++]) : '{}'));
  const rest = args.slice(i);
  return rest.length > 0 ? `${text} ${rest.map((a) => (typeof a === 'object' ? JSON.stringify(a) : format('%s', a))).join(' ')}` : text;
}

/**
 * Line logger. Supports SLF4J-style "{}" placeholders; any extra arguments are
 * appended as JSON so structured context can be passed as an object.
 */
export class Logger {
  constructor(private readonly component: string, private readonly sink: (line: string) => void = console.log) {}

  debug(message: string, ...args: unknown[]): void {
    this.write('debug', message, args);
  }

  info(message: string, ...args: unknown[]): void {
    this.write('info', message, args);
  }

  warn(message: string, ...args: unknown[]): void {
    this.write('warn', message, args);
  }

  error(message: string, ...args: unknown[]): void {
    this.write('error', message, args);
  }

  private write(level: LogLevel, message: string, args: unknown[]): void {
    this.sink(`${new Date().toISOString()} ${level.toUpperCase()} [${this.component}] ${render(message, args)}`);
  }
}
