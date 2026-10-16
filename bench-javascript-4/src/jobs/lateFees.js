'use strict';

const db = require('../db');
const { applyLateFee, UNPAID } = require('../services/fineService');

const FINE_LOCK_NAMESPACE = 2;
const LATE_FEE_DIVISOR = 10;

async function applyDailyLateFees(now = new Date()) {
  const startOfDay = new Date(now.toISOString().slice(0, 10));
  const overdue = await db('fines')
    .where({ status: UNPAID })
    .where('due_at', '<', now)
    .where((q) => q.whereNull('late_fee_applied_at').orWhere('late_fee_applied_at', '<', startOfDay))
    .select('id', 'amount_cents');
  let applied = 0;
  for (const fine of overdue) {
    const fee = Math.floor(fine.amount_cents / LATE_FEE_DIVISOR);
    const total = await db.transaction(async (trx) => {
      await trx.raw('select pg_advisory_xact_lock(?, ?)', [FINE_LOCK_NAMESPACE, fine.id]);
      return applyLateFee(trx, fine.id, fee, startOfDay);
    });
    if (total !== null) applied += 1;
  }
  return applied;
}

if (require.main === module) {
  applyDailyLateFees()
    .then((count) => console.log(`late fees applied to ${count} fines`))
    .catch((err) => {
      console.error(err);
      process.exitCode = 1;
    })
    .finally(() => db.destroy());
}

module.exports = { applyDailyLateFees };
