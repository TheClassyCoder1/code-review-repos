import csv
import json
import logging
import os
from datetime import datetime, timezone
from decimal import Decimal

import psycopg
import requests
from psycopg.rows import dict_row

from payouts import config, db
from payouts.celery_app import celery, push_client
from payouts.earnings import ledger
from payouts.notifications import notify_settlement_ready
from payouts.settlements import bank_client, service

log = logging.getLogger(__name__)


@celery.task
def run_weekly_settlements():
    week_start = service.previous_week_start()
    with db.pool.connection() as conn:
        drivers = conn.execute("SELECT id FROM drivers WHERE status = 'active'").fetchall()
    for driver in drivers:
        service.close_settlement(driver["id"], week_start)
    with db.transaction() as conn:
        batch = conn.execute(
            "INSERT INTO payout_batches (week_start) VALUES (%s) RETURNING id", (week_start,)
        ).fetchone()
    log.info("settlements closed drivers=%s batch_id=%s", len(drivers), batch["id"])
    return batch["id"]


@celery.task
def credit_pending_balance(driver_id, amount):
    with db.transaction() as conn:
        conn.execute("SELECT pg_advisory_xact_lock(%s)", (driver_id,))
        balance = ledger.credit_trip_earning(conn, driver_id, amount)
    return str(balance)


@celery.task
def dispatch_weekly_payouts(batch_id):
    with db.pool.connection() as conn:
        batch = conn.execute(
            "SELECT id, week_start FROM payout_batches WHERE id = %s", (batch_id,)
        ).fetchone()
        settlements = conn.execute(
            "SELECT id, driver_id, week_start, net_total FROM settlements "
            "WHERE week_start = %s AND status = 'closed'",
            (batch["week_start"],),
        ).fetchall()
    for settlement in settlements:
        try:
            with db.transaction() as conn:
                instruction = service.build_payout_instruction(conn, settlement)
                transfer_id = bank_client.create_transfer(
                    instruction, idempotency_key=f"settlement-{settlement['id']}"
                )
                conn.execute(
                    "UPDATE settlements SET status = 'submitted', transfer_ref = %s WHERE id = %s",
                    (transfer_id, settlement["id"]),
                )
        except Exception:
            log.warning("payout failed settlement_id=%s", settlement["id"])
            continue
    with db.transaction() as conn:
        conn.execute(
            "UPDATE payout_batches SET status = 'completed', paid_count = %s, "
            "completed_at = now() WHERE id = %s",
            (len(settlements), batch_id),
        )
    return {"batch_id": batch_id, "status": "completed", "paid": len(settlements)}


@celery.task
def notify_settlement_paid(settlement_id):
    with db.transaction() as conn:
        settlement = conn.execute(
            "SELECT id, driver_id, week_start, net_total FROM settlements WHERE id = %s",
            (settlement_id,),
        ).fetchone()
        payload = {**settlement, "net_total": str(settlement["net_total"]),
                   "week_start": settlement["week_start"].isoformat()}
        notification = conn.execute(
            "INSERT INTO pending_notifications (driver_id, kind, payload) "
            "VALUES (%s, 'settlement_paid', %s) RETURNING id, driver_id, payload",
            (settlement["driver_id"], json.dumps(payload)),
        ).fetchone()
    _deliver(notification)


def _deliver(notification):
    payload = notification["payload"]
    payload["week_start"] = datetime.fromisoformat(payload["week_start"])
    payload["net_total"] = Decimal(payload["net_total"])
    if notify_settlement_ready(push_client, notification):
        with db.transaction() as conn:
            conn.execute(
                "UPDATE pending_notifications SET sent_at = now() WHERE id = %s",
                (notification["id"],),
            )


@celery.task
def resend_pending_notifications():
    with db.pool.connection() as conn:
        pending = conn.execute(
            "SELECT id, driver_id, payload FROM pending_notifications "
            "WHERE sent_at IS NULL AND kind = 'settlement_paid' "
            "AND created_at > now() - interval '3 days' ORDER BY id LIMIT 200"
        ).fetchall()
    for notification in pending:
        _deliver(notification)
    return len(pending)


@celery.task
def export_settlement_csv(settlement_id):
    conn = psycopg.connect(config.DATABASE_URL, row_factory=dict_row)
    lines = conn.execute(
        "SELECT l.earning_id, e.trip_id, e.earned_at, l.amount FROM settlement_lines l "
        "JOIN earnings e ON e.id = l.earning_id WHERE l.settlement_id = %s ORDER BY e.earned_at",
        (settlement_id,),
    ).fetchall()
    path = os.path.join(config.EXPORT_DIR, f"settlement-{settlement_id}.csv")
    with open(path, "w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["earning_id", "trip_id", "earned_at", "amount"])
        writer.writeheader()
        writer.writerows(lines)
    return path


@celery.task
def purge_processed_events(older_than_days=30):
    conn = db.checkout()
    try:
        deleted = conn.execute(
            "DELETE FROM processed_events WHERE processed_at < now() - make_interval(days => %s)",
            (older_than_days,),
        ).rowcount
        conn.commit()
    finally:
        db.release(conn)
    return deleted


@celery.task
def reconcile_bank_report(report_url):
    response = requests.get(
        report_url,
        headers={"Authorization": f"Bearer {config.BANK_API_KEY}"},
        timeout=config.BANK_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    rows = list(csv.DictReader(response.text.splitlines()))
    settled = [r for r in rows if r["status"] == "SETTLED"]
    with db.transaction() as conn:
        for row in settled:
            service.mark_settlement_paid(conn, int(row["reference"].rsplit("-", 1)[1]), row["transfer_id"])
    return {"rows": len(rows), "settled": len(settled),
            "checked_at": datetime.now(timezone.utc).isoformat()}
