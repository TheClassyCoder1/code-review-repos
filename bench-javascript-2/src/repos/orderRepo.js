'use strict';

const { pool, withTransaction } = require('../db');
const { httpError } = require('../lib/validate');

async function insertOrder(customerId, input, quoteFee) {
  return withTransaction(async (client) => {
    const { rows: [restaurant] } = await client.query(
      'SELECT id, lat, lng FROM restaurants WHERE id = $1 AND is_open',
      [input.restaurantId],
    );
    if (!restaurant) throw httpError(422, 'restaurant is closed or does not exist');
    const feeCents = quoteFee(restaurant, input.dropoff);
    const { rows: [order] } = await client.query(
      `INSERT INTO orders (customer_id, restaurant_id, dropoff_lat, dropoff_lng, fee_cents, status)
       VALUES ($1, $2, $3, $4, $5, 'placed')
       RETURNING id, status, fee_cents, created_at`,
      [customerId, restaurant.id, input.dropoff.lat, input.dropoff.lng, feeCents],
    );
    for (const item of input.items) {
      await client.query(
        'INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES ($1, $2, $3)',
        [order.id, item.menuItemId, item.quantity],
      );
    }
    return order;
  });
}

async function listForCustomer(customerId, { limit, offset }) {
  const { rows } = await pool.query(
    `SELECT id, restaurant_id, status, fee_cents, tip_cents, created_at
       FROM orders WHERE customer_id = $1
      ORDER BY created_at DESC, id DESC
      LIMIT $2 OFFSET $3`,
    [customerId, limit, offset],
  );
  return rows;
}

async function cancelForCustomer(orderId, customerId) {
  const { rows } = await pool.query(
    `UPDATE orders SET status = 'cancelled', cancelled_at = now()
      WHERE id = $1 AND customer_id = $2 AND status IN ('placed', 'assigned')
      RETURNING id, courier_id`,
    [orderId, customerId],
  );
  return rows[0] || null;
}

async function addTip(orderId, customerId, extraCents) {
  const { rows } = await pool.query(
    'SELECT tip_cents FROM orders WHERE id = $1 AND customer_id = $2',
    [orderId, customerId],
  );
  if (!rows[0]) throw httpError(404, 'order not found');
  const tipCents = rows[0].tip_cents + extraCents;
  await pool.query('UPDATE orders SET tip_cents = $2 WHERE id = $1', [orderId, tipCents]);
  return tipCents;
}

module.exports = { insertOrder, listForCustomer, cancelForCustomer, addTip };
