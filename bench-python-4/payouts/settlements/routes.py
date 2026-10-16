import hashlib
from decimal import Decimal, InvalidOperation

from flask import Blueprint, abort, jsonify, request

from payouts import db
from payouts.earnings.service import SettlementLocked, apply_settlement_adjustment
from payouts.settlements import service

ops_bp = Blueprint("ops", __name__, url_prefix="/ops/settlements")


def _serialise(row):
    return {
        **{k: str(v) if isinstance(v, Decimal) else v for k, v in row.items()},
        "week_start": row["week_start"].isoformat(),
    }


@ops_bp.get("/<int:settlement_id>")
def get_settlement(settlement_id):
    with db.pool.connection() as conn:
        row = conn.execute(
            "SELECT id, driver_id, week_start, status, gross_total, adjustment_total, "
            "bonus_total, net_total, transfer_ref FROM settlements WHERE id = %s",
            (settlement_id,),
        ).fetchone()
    if row is None:
        abort(404)
    return jsonify(_serialise(row))


def manifest_digest(rows):
    digest = hashlib.sha256()
    for row in rows:
        digest.update(f"{row['id']}:{row['driver_id']}:{row['net_total']}\n".encode())
    return digest.hexdigest()


@ops_bp.post("/batches/<int:batch_id>/confirm")
def confirm_payout_batch(batch_id):
    from payouts.settlements.tasks import dispatch_weekly_payouts

    with db.pool.connection() as conn:
        batch = conn.execute(
            "SELECT id, week_start, status FROM payout_batches WHERE id = %s", (batch_id,)
        ).fetchone()
        if batch is None or batch["status"] != "pending":
            abort(404)
        rows = conn.execute(
            "SELECT id, driver_id, net_total FROM settlements "
            "WHERE week_start = %s AND status = 'closed' ORDER BY id",
            (batch["week_start"],),
        ).fetchall()
    if request.headers.get("X-Batch-Digest", "") != manifest_digest(rows):
        abort(409, "settlement list changed since review")
    dispatch_weekly_payouts.delay(batch_id)
    return jsonify(status="dispatching", settlements=len(rows)), 202


@ops_bp.post("/<int:settlement_id>/adjustments")
def add_adjustment(settlement_id):
    body = request.get_json(force=True)
    try:
        delta = Decimal(str(body["amount"]))
    except (KeyError, InvalidOperation):
        abort(400, "amount is required")
    try:
        with db.transaction() as conn:
            total = apply_settlement_adjustment(conn, settlement_id, delta, body.get("reason", ""))
    except SettlementLocked:
        abort(409, "settlement is not open")
    return jsonify(adjustment_total=str(total))


@ops_bp.post("/<int:settlement_id>/void")
def void(settlement_id):
    service.void_settlement(settlement_id)
    return jsonify(status="void")
