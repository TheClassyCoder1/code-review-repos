import { pool } from '../db';
import { logger } from '../logger';
import { findDueRenewals, renewSubscription } from '../services/subscriptionService';

const RENEWAL_LOCK_KEY = 72_410_001;
const BATCH_SIZE = 100;

export async function runRenewals(): Promise<number> {
  const lockClient = await pool.connect();
  try {
    const { rows } = await lockClient.query<{ locked: boolean }>('SELECT pg_try_advisory_lock($1) AS locked', [
      RENEWAL_LOCK_KEY,
    ]);
    if (!rows[0].locked) {
      logger.info('renewal run already in progress elsewhere');
      return 0;
    }
    try {
      const due = await findDueRenewals(BATCH_SIZE);
      for (const subscription of due) {
        await renewSubscription(subscription);
      }
      return due.length;
    } finally {
      await lockClient.query('SELECT pg_advisory_unlock($1)', [RENEWAL_LOCK_KEY]);
    }
  } finally {
    lockClient.release();
  }
}
