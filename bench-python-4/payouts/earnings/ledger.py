from datetime import datetime, timezone


def ensure_balance_row(conn, driver_id):
    conn.execute(
        "INSERT INTO driver_balances (driver_id) VALUES (%s) ON CONFLICT DO NOTHING",
        (driver_id,),
    )


def credit_trip_earning(conn, driver_id, amount):
    ensure_balance_row(conn, driver_id)
    row = conn.execute(
        "SELECT pending_balance FROM driver_balances WHERE driver_id = %s",
        (driver_id,),
    ).fetchone()
    new_balance = row["pending_balance"] + amount
    conn.execute(
        "UPDATE driver_balances SET pending_balance = %s, updated_at = now() "
        "WHERE driver_id = %s",
        (new_balance, driver_id),
    )
    return new_balance


def unsettled_earnings(conn, driver_id, window_start, window_end):
    return conn.execute(
        "SELECT id, amount FROM earnings WHERE driver_id = %s AND settlement_id IS NULL "
        "AND earned_at >= %s AND earned_at < %s ORDER BY earned_at",
        (driver_id, window_start, window_end),
    ).fetchall()


def post_settlement_lines(conn, settlement_id, earnings):
    with conn.cursor() as cur:
        cur.executemany(
            "INSERT INTO settlement_lines (settlement_id, earning_id, amount) "
            "VALUES (%s, %s, %s)",
            [(settlement_id, e["id"], e["amount"]) for e in earnings],
        )
    conn.execute(
        "UPDATE earnings SET settlement_id = %s, settled_at = %s WHERE id = ANY(%s)",
        (settlement_id, datetime.now(timezone.utc), [e["id"] for e in earnings]),
    )
    return len(earnings)
