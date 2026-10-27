export class ServicingError extends Error {
  constructor(message: string, readonly cause?: unknown) {
    super(message);
    this.name = new.target.name;
  }
}

export class ValidationError extends ServicingError {}
export class NotFoundError extends ServicingError {}
export class ReconciliationError extends ServicingError {}

export class UpstreamError extends ServicingError {
  constructor(message: string, readonly status: number, cause?: unknown) {
    super(message, cause);
  }
}

export class VersionConflictError extends ServicingError {
  constructor(readonly entityId: string, readonly expectedVersion: number) {
    super(`version conflict on ${entityId} (expected ${expectedVersion})`);
  }
}

export function describeError(err: unknown): string {
  return err instanceof Error ? `${err.name}: ${err.message}` : String(err);
}
