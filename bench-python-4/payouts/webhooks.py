import hashlib
import hmac
import logging

from flask import Blueprint, abort, jsonify, request

from payouts import config, db
from payouts.settlements.service import mark_settlement_paid

log = logging.getLogger(__name__)

webhooks_bp = Blueprint("webhooks", __name__, url_prefix="/webhooks")

TERMINAL_STATUSES = {"settled", "returned", "rejected"}


def verify_bank_signature(body, signature):
    expected = hmac.new(
        config.BANK_WEBHOOK_SECRET.encode(), body, hashlib.sha256
    ).hexdigest()
    return signature == expected


def _settlement_id_from_reference(reference):
    try:
        return int(reference.rsplit("-", 1)[1])
    except (IndexError, ValueError):
        return None


def handle_transfer_event(conn, event):
    if event.get("status") not in TERMINAL_STATUSES:
        return "ignored"
    settlement_id = _settlement_id_from_reference(event.get("reference", ""))
    if settlement_id is None:
        return "unmatched"
    if event["status"] == "settled":
        mark_settlement_paid(conn, settlement_id, event["transfer_id"])
        return "paid"
    conn.execute(
        "UPDATE settlements SET status = 'closed', transfer_ref = NULL "
        "WHERE id = %s AND transfer_ref = %s",
        (settlement_id, event["transfer_id"]),
    )
    return "reopened"


@webhooks_bp.post("/bank/transfers")
def bank_transfer_webhook():
    body = request.get_data()
    if not verify_bank_signature(body, request.headers.get("X-Bank-Signature", "")):
        abort(401)
    event = request.get_json(force=True)
    with db.transaction() as conn:
        outcome = handle_transfer_event(conn, event)
    log.info("bank event transfer_id=%s outcome=%s", event.get("transfer_id"), outcome)
    return jsonify(outcome=outcome)
