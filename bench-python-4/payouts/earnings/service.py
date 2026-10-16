from decimal import Decimal

from payouts.earnings.calculator import to_money


class SettlementLocked(Exception):
    pass


def _open_settlement(conn, settlement_id):
    row = conn.execute(
        "SELECT id, status, adjustment_total, bonus_total FROM settlements WHERE id = %s",
        (settlement_id,),
    ).fetchone()
    if row is None or row["status"] != "open":
        raise SettlementLocked(settlement_id)
    return row


def apply_settlement_adjustment(conn, settlement_id, delta, reason):
    settlement = _open_settlement(conn, settlement_id)
    new_total = settlement["adjustment_total"] + to_money(delta)
    conn.execute(
        "UPDATE settlements SET adjustment_total = %s WHERE id = %s",
        (new_total, settlement_id),
    )
    conn.execute(
        "INSERT INTO pending_notifications (driver_id, kind, payload) "
        "SELECT driver_id, 'adjustment', jsonb_build_object('reason', %s::text, 'amount', %s::text) "
        "FROM settlements WHERE id = %s",
        (reason, str(to_money(delta)), settlement_id),
    )
    return new_total


def summarize_week(conn, driver_id, window_start, window_end):
    row = conn.execute(
        "SELECT count(*) AS trips, coalesce(sum(amount), 0) AS total FROM earnings "
        "WHERE driver_id = %s AND earned_at >= %s AND earned_at < %s",
        (driver_id, window_start, window_end),
    ).fetchone()
    return {"trips": row["trips"], "total": to_money(row["total"])}


def credit_settlement_bonus(conn, settlement_id, bonus):
    if bonus <= Decimal("0"):
        return None
    row = conn.execute(
        "UPDATE settlements SET bonus_total = bonus_total + %s "
        "WHERE id = %s AND status = 'open' RETURNING bonus_total",
        (to_money(bonus), settlement_id),
    ).fetchone()
    if row is None:
        raise SettlementLocked(settlement_id)
    return row["bonus_total"]
