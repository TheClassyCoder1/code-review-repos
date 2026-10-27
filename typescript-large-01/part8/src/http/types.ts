export interface AuthenticatedUser {
  id: number;
  userName: string;
  permissions: string[];
}

export interface HttpRequest {
  method: string;
  path: string;
  query: Record<string, string | undefined>;
  body: Record<string, unknown>;
  headers: Record<string, string | undefined>;
  user?: AuthenticatedUser;
}

export interface HttpResponse {
  status(code: number): HttpResponse;
  setHeader(name: string, value: string): void;
  json(body: unknown): void;
  send(body: string): void;
}

export class CommonResult<T> {
  private constructor(readonly code: number, readonly message: string, readonly data: T | null) {}

  static success<T>(data: T): CommonResult<T> {
    return new CommonResult<T>(200, 'ok', data);
  }

  static failed<T>(message: string): CommonResult<T> {
    return new CommonResult<T>(500, message, null);
  }
}

export class HttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}
