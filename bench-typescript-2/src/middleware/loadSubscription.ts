import { NextFunction, Request, Response } from 'express';
import { pool } from '../db';
import { Subscription } from '../types';

export async function loadSubscription(req: Request, res: Response, next: NextFunction): Promise<void> {
  const { rows } = await pool.query<Subscription>(
    'SELECT * FROM subscriptions WHERE id = $1 AND account_id = $2',
    [req.params.subscriptionId, req.account!.id],
  );
  if (rows.length === 0) {
    res.status(404).json({ error: 'subscription not found' });
    return;
  }
  res.locals.subscription = rows[0];
  next();
}
