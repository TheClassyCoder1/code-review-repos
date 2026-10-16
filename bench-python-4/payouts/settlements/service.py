from datetime import date, datetime, time, timedelta, timezone

from payouts import db
from payouts.earnings import ledger
from payouts.earnings.calculator import settlement_net_total, weekly_incentive
from payouts.earnings.service import credit_settlement_bonus

PAYOUT_CURRENCY = "EUR"


class MissingPayoutAccount(Exception):
    pass


def week_start_for(day):
    if isinstance(day, datetime):
        day = day.astimezone(timezone.utc).date()
    return day - timedelta(days=day.weekday())


def week_bounds(week_start):
    start = datetime.combine(week_start, time.min, tzinfo=timezone.utc)
    return start, start + timedelta(days=7)


def open_weekly_settlement(conn, driver_id, week_start):
    row = conn.execute(
        "SELECT id FROM settlements WHERE driver_id = %s AND week_start = %s",
        (driver_id, week_start),
    ).fetchone()
    if row is not None:
        return row["id"]
    created = conn.execute(
        "INSERT INTO settlements (driver_id, week_start) VALUES (%s, %s) RETURNING id",
        (driver_id, week_start),
    ).fetchone()
    return created["id"]


def previous_week_start(today=None):
    today = today or datetime.now(timezone.utc).date()
    return week_start_for(today) - timedelta(days=7)


def collect_week_earnings(conn, driver_id, week_start):
    window_start, window_end = week_bounds(week_start)
    return conn.execute(
        "SELECT id, amount FROM earnings WHERE driver_id = %s "
        "AND earned_at BETWEEN %s AND %s ORDER BY earned_at",
        (driver_id, window_start, window_end),
    ).fetchall()


def close_settlement(driver_id, week_start):
    with db.transaction() as conn:
        settlement_id = open_weekly_settlement(conn, driver_id, week_start)
        earnings = collect_week_earnings(conn, driver_id, week_start)
        ledger.post_settlement_lines(conn, settlement_id, earnings)
        gross = sum((e["amount"] for e in earnings), start=0)
        credit_settlement_bonus(conn, settlement_id, weekly_incentive(len(earnings)))
        row = conn.execute(
            "SELECT adjustment_total, bonus_total FROM settlements WHERE id = %s",
            (settlement_id,),
        ).fetchone()
        net = settlement_net_total(gross, row["adjustment_total"], row["bonus_total"])
        conn.execute(
            "UPDATE settlements SET gross_total = %s, net_total = %s, status = 'closed' "
            "WHERE id = %s",
            (gross, net, settlement_id),
        )
    return settlement_id


def build_payout_instruction(conn, settlement):
    account = conn.execute(
        "SELECT iban, holder_name FROM payout_accounts "
        "WHERE driver_id = %s AND verified_at IS NOT NULL",
        (settlement["driver_id"],),
    ).fetchone()
    return {
        "amount": settlement["net_total"],
        "currency": PAYOUT_CURRENCY,
        "iban": account["iban"],
        "holder_name": account["holder_name"],
        "reference": f"WEEKLY-{settlement['week_start']:%Y%m%d}-{settlement['id']}",
    }


def mark_settlement_paid(conn, settlement_id, transfer_ref):
    conn.execute(
        "UPDATE settlements SET status = 'paid', transfer_ref = %s, paid_at = now() "
        "WHERE id = %s AND status IN ('closed', 'submitted')",
        (transfer_ref, settlement_id),
    )


def void_settlement(settlement_id):
    with db.pool.connection() as conn:
        conn.execute(
            "UPDATE earnings SET settlement_id = NULL, settled_at = NULL "
            "WHERE settlement_id = %s",
            (settlement_id,),
        )
    with db.pool.connection() as conn:
        conn.execute("DELETE FROM settlement_lines WHERE settlement_id = %s", (settlement_id,))
        conn.execute(
            "UPDATE settlements SET status = 'void', gross_total = 0, net_total = 0 "
            "WHERE id = %s AND status = 'closed'",
            (settlement_id,),
        )
    return settlement_id
