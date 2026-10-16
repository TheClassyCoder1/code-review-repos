from decimal import Decimal

import psycopg
from flask import Blueprint, abort, g, jsonify, request

from payouts import db
from payouts.drivers import repository
from payouts.earnings.calculator import instant_cashout_amount, is_cashout_allowed

drivers_bp = Blueprint("drivers", __name__, url_prefix="/drivers/me")

ALLOWED_EVIDENCE_TYPES = {"image/jpeg", "image/png", "application/pdf"}


@drivers_bp.get("")
def profile():
    with db.pool.connection() as conn:
        name = repository.get_driver_name(conn, g.driver_id)
        account = repository.get_payout_account(conn, g.driver_id)
    return jsonify(
        driver_id=g.driver_id,
        full_name=name,
        payout_account=account,
        payout_ready=bool(account and account["verified"]),
    )


def _limit_arg():
    try:
        value = int(request.args.get("limit", 20))
    except ValueError:
        abort(400, "limit must be an integer")
    return max(1, min(value, 100))


@drivers_bp.get("/trips")
def driver_trips():
    limit = _limit_arg()
    before = repository.parse_cursor(request.args.get("before"))
    with db.pool.connection() as conn:
        rows = repository.list_driver_trips(conn, g.driver_id, before, limit)
    has_more = len(rows) > limit
    items = rows[:limit]
    return jsonify(
        items=[{**r, "completed_at": r["completed_at"].isoformat()} for r in items],
        next_cursor=items[-1]["completed_at"].isoformat() if has_more else None,
    )


@drivers_bp.post("/payout-account")
def add_payout_account():
    body = request.get_json(force=True)
    iban = body.get("iban") or ""
    holder = body.get("holder_name") or ""
    if len(iban.replace(" ", "")) < 15 or not holder.strip():
        abort(400, "iban and holder_name are required")
    try:
        with db.transaction() as conn:
            account_id = repository.register_payout_account(conn, g.driver_id, iban, holder)
    except (repository.DuplicateAccount, psycopg.errors.UniqueViolation):
        abort(409, "payout account already registered")
    return jsonify(id=account_id), 201


@drivers_bp.post("/trips/<int:trip_id>/disputes/evidence")
def upload_dispute_evidence(trip_id):
    upload = request.files.get("evidence")
    if upload is None or upload.mimetype not in ALLOWED_EVIDENCE_TYPES:
        abort(400, "evidence must be a jpeg, png or pdf file")
    data = upload.read()
    with db.transaction() as conn:
        trip = conn.execute(
            "SELECT id FROM trips WHERE id = %s AND driver_id = %s",
            (trip_id, g.driver_id),
        ).fetchone()
        if trip is None:
            abort(404)
        conn.execute(
            "INSERT INTO dispute_evidence (trip_id, driver_id, file_name, content) "
            "VALUES (%s, %s, %s, %s)",
            (trip_id, g.driver_id, upload.filename, data),
        )
    return jsonify(status="received", size=len(data)), 201


@drivers_bp.get("/cashout-quote")
def cashout_quote():
    with db.pool.connection() as conn:
        row = conn.execute(
            "SELECT pending_balance FROM driver_balances WHERE driver_id = %s",
            (g.driver_id,),
        ).fetchone()
    balance = row["pending_balance"] if row else Decimal("0.00")
    if not is_cashout_allowed(balance):
        abort(422, "balance below instant cashout minimum")
    return jsonify(instant_cashout_amount(balance))
