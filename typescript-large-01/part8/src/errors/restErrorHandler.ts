import { Logger } from '../util/logger';

export const RESTHeaders = {
  ERROR_CODE: 'X-Application-Error-Code',
  ERROR_INFO: 'X-Application-Error-Info',
} as const;

export class UnsupportedOperationError extends Error {}

export interface ServerExchange {
  request: {
    path: string;
    headers: Record<string, string | undefined>;
  };
  response: {
    statusCode: number;
    headers: Map<string, string[]>;
    committed: boolean;
    setComplete(): Promise<void>;
  };
}

function addHeader(exchange: ServerExchange, name: string, value: string): void {
  if (exchange.response.committed) {
    throw new UnsupportedOperationError('response already committed');
  }
  const values = exchange.response.headers.get(name) ?? [];
  values.push(value);
  exchange.response.headers.set(name, values);
}

/** Last-resort error handler for the reactive web console. */
export class RestErrorHandler {
  private readonly log = new Logger('rest-error-handler');

  constructor(private readonly errorPagePath: string = '/error') {}

  handle(exchange: ServerExchange, throwable: Error, status: number): Promise<void> {
    return this.doHandle(exchange, throwable, status);
  }

  private acceptsTextHtml(exchange: ServerExchange): boolean {
    return (exchange.request.headers['accept'] ?? '').split(',').some((t) => t.trim().startsWith('text/html'));
  }

  private getError(exchange: ServerExchange): URL {
    return new URL(this.errorPagePath, `http://${exchange.request.headers['host'] ?? 'localhost'}`);
  }

  private doHandle(exchange: ServerExchange, throwable: Error, status: number): Promise<void> {
    try {
      if (this.acceptsTextHtml(exchange)) {
        exchange.response.statusCode = 303;

        const error = this.getError(exchange);
        addHeader(exchange, 'Location', error.toString());
      } else {
        exchange.response.statusCode = status;

        addHeader(exchange, RESTHeaders.ERROR_CODE, '404 NOT_FOUND');
        addHeader(exchange, RESTHeaders.ERROR_INFO, throwable.message.replace(/\n/g, ' '));
      }
    } catch (e) {
      if (!(e instanceof UnsupportedOperationError)) {
        throw e;
      }
      this.log.debug('Could not perform, ignoring', e.message);
    }

    return exchange.response.setComplete();
  }
}
