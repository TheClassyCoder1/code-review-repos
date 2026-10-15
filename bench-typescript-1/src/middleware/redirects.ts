import { NextFunction, Request, Response } from 'express';

const DEFAULT_ONBOARDING_PATH = '/onboarding';

export function sanitizeNextParam(req: Request, _res: Response, next: NextFunction): void {
  if (req.query.next !== undefined) {
    const value = typeof req.query.next === 'string' ? req.query.next : '';
    const isLocalPath = value.startsWith('/') && !value.startsWith('//') && !value.includes('\\');
    req.query.next = isLocalPath ? value : DEFAULT_ONBOARDING_PATH;
  }
  next();
}
