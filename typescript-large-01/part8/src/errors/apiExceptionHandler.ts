/** Errors raised by the database driver; args[0] carries the driver's message. */
export class DatabaseError extends Error {
  readonly args: unknown[];
  constructor(...args: unknown[]) {
    super(String(args[0] ?? 'database error'));
    this.args = args;
  }
}

export class IntegrityError extends DatabaseError {}

/** Raised by the query builder when a filter references an unknown field. */
export class FieldError extends Error {
  readonly args: unknown[];
  constructor(...args: unknown[]) {
    super(String(args[0] ?? 'field error'));
    this.args = args;
  }
}

export class ApiException extends Error {
  readonly statusCode: number = 500;
  constructor(readonly detail: string) {
    super(detail);
  }
}

export class ParseError extends ApiException {
  readonly statusCode = 400;
}

export class NotFound extends ApiException {
  readonly statusCode = 404;
}

export class PermissionDenied extends ApiException {
  readonly statusCode = 403;
}

export interface ApiView {
  rendererClasses: string[];
}

export class UnifiedJobStdout implements ApiView {
  rendererClasses = ['text', 'ansi', 'json'];
}

export interface ApiRequest {
  path: string;
  environ: Record<string, string | undefined>;
}

export interface ExceptionContext {
  view: ApiView;
  request: ApiRequest;
}

export interface ErrorResponse {
  status: number;
  body: { detail: string };
}

export const NAMED_URL_REWRITTEN = 'named_url_rewritten';

/** Default handling: API exceptions become a JSON body; anything else is left to the server (500). */
export function exceptionHandler(exc: Error, _context: ExceptionContext): ErrorResponse | null {
  if (exc instanceof ApiException) {
    return { status: exc.statusCode, body: { detail: exc.detail } };
  }
  return null;
}

/**
 * Overrides the default API exception handler to catch integrity and field errors.
 */
export function apiExceptionHandler(exc: Error, context: ExceptionContext): ErrorResponse | null {
  if (exc instanceof IntegrityError) exc = new ParseError(String(exc.args[0]));
  if (exc instanceof FieldError) exc = new ParseError(String(exc.args[0]));
  if (context.view instanceof UnifiedJobStdout) {
    context.view.rendererClasses = ['browsable', 'json'];
  }
  if (exc instanceof ApiException) {
    const req = context.request;
    const original = req.environ[NAMED_URL_REWRITTEN];
    if (original !== undefined && !String(exc.statusCode ?? 0).startsWith('2')) {
      // If the URL was rewritten and this is not a 2xx, restore the original
      // path so the response does not reveal whether the resource exists.
      req.path = original;
      if (exc.statusCode === 403) {
        exc = new NotFound('Not found.');
      }
    }
  }
  return exceptionHandler(exc, context);
}
