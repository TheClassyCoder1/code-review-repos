import { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { getListingOwnerId } from '../services/listingService';

export interface AuthUser {
  id: string;
  sellerId: string;
  role: 'seller' | 'admin';
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: AuthUser;
    }
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction): void {
  const header = req.headers.authorization ?? '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : req.cookies?.session;
  try {
    req.user = jwt.verify(String(token ?? ''), config.jwtSecret) as unknown as AuthUser;
    next();
  } catch {
    res.status(401).json({ error: 'unauthorized' });
  }
}

export function requireAdmin(req: Request, res: Response, next: NextFunction): void {
  if (req.user?.role !== 'admin') {
    res.status(403).json({ error: 'forbidden' });
    return;
  }
  next();
}

export async function requireListingOwner(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const ownerId = await getListingOwnerId(req.params.listingId);
    if (!ownerId || ownerId !== req.user?.sellerId) {
      res.status(404).json({ error: 'listing not found' });
      return;
    }
    next();
  } catch (err) {
    next(err);
  }
}
