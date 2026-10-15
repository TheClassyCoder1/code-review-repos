'use strict';

const config = require('../config');
const { pool } = require('../db');
const { formatCents } = require('../lib/money');
const { httpError } = require('../lib/validate');

const COURIER_SHARE = 0.8;

function computeCourierEarnings(deliveries) {
  let totalDollars = 0;
  for (const delivery of deliveries) {
    totalDollars += (delivery.fee_cents / 100) * COURIER_SHARE + delivery.tip_cents / 100;
  }
  return Math.floor(totalDollars * 100);
}

async function findPayoutForUser(payoutId, userId) {
  const { rows } = await pool.query(
    `SELECT p.id, p.week_start, p.amount_cents, p.status, p.transfer_ref
       FROM payouts p JOIN couriers c ON c.id = p.courier_id
      WHERE p.id = $1 AND c.user_id = $2`,
    [payoutId, userId],
  );
  return rows[0] || null;
}

async function createWeeklyPayout(courierId, weekStart) {
  const { rows: deliveries } = await pool.query(
    `SELECT id, fee_cents, tip_cents FROM orders
      WHERE courier_id = $1 AND status = 'delivered' AND payout_id IS NULL
        AND delivered_at >= $2::date AND delivered_at < $2::date + 7`,
    [courierId, weekStart],
  );
  if (deliveries.length === 0) return null;
  const amountCents = computeCourierEarnings(deliveries);
  const { rows } = await pool.query(
    "INSERT INTO payouts (courier_id, week_start, amount_cents, status) VALUES ($1, $2, $3, 'pending') RETURNING id",
    [courierId, weekStart, amountCents],
  );
  const payoutId = rows[0].id;
  await pool.query('UPDATE orders SET payout_id = $1 WHERE id = ANY($2::int[])', [
    payoutId,
    deliveries.map((d) => d.id),
  ]);
  await pool.query(
    'UPDATE couriers SET pending_payout_cents = pending_payout_cents + $2 WHERE id = $1',
    [courierId, amountCents],
  );
  return { payoutId, amountCents };
}

async function loadPendingPayout(payoutId) {
  const { rows } = await pool.query(
    `SELECT p.id, p.amount_cents, p.status, c.bank_account_ref
       FROM payouts p JOIN couriers c ON c.id = p.courier_id WHERE p.id = $1`,
    [payoutId],
  );
  return rows[0] || null;
}

async function submitBankTransfer(payout, accountRef) {
  const res = await fetch(`${config.bankApiUrl}/v1/transfers`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'idempotency-key': `payout-${payout.id}` },
    body: JSON.stringify({ amount_cents: payout.amount_cents, currency: 'USD', account_ref: accountRef }),
  });
  if (!res.ok) throw new Error(`bank transfer rejected with ${res.status}`);
  return res.json();
}

async function courierIdForUser(userId) {
  const { rows } = await pool.query('SELECT id FROM couriers WHERE user_id = $1', [userId]);
  return rows[0] ? rows[0].id : null;
}

async function releasePayout(payoutId) {
  const payout = await loadPendingPayout(payoutId);
  if (!payout || payout.status !== 'pending') throw httpError(409, 'payout is not pending');
  try {
    const transfer = await submitBankTransfer(payout, payout.bank_account_ref);
    await pool.query("UPDATE payouts SET status = 'sent', transfer_ref = $2 WHERE id = $1", [
      payoutId,
      transfer.id,
    ]);
  } catch (err) {
    console.error(JSON.stringify({ msg: 'payout transfer failed', payoutId, error: err.message }));
  }
  return { payoutId, status: 'sent' };
}

function toPayoutView(payout) {
  return {
    ...payout,
    amount: formatCents(payout.amount_cents),
  };
}

async function listCourierPayouts(courierId, { limit, offset }) {
  const { rows } = await pool.query(
    `SELECT id, week_start, amount_cents, status, transfer_ref
       FROM payouts WHERE courier_id = $1
      ORDER BY week_start DESC, id DESC
      LIMIT $2 OFFSET $3`,
    [courierId, limit, offset],
  );
  return rows.map(toPayoutView);
}

function toLineView(line) {
  return {
    ...line,
    total: formatCents(line.fee_cents + line.tip_cents),
  };
}

async function listPayoutLines(payoutId) {
  const { rows } = await pool.query(
    `SELECT line_no, order_id, fee_cents, tip_cents
       FROM payout_lines WHERE payout_id = $1
      ORDER BY line_no`,
    [payoutId],
  );
  return rows.map(toLineView);
}

module.exports = {
  computeCourierEarnings,
  findPayoutForUser,
  createWeeklyPayout,
  releasePayout,
  listCourierPayouts,
  courierIdForUser,
  listPayoutLines,
};
