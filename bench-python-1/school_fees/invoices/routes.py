from datetime import date
from decimal import Decimal, InvalidOperation

from flask import (
    Blueprint,
    abort,
    current_app,
    g,
    jsonify,
    redirect,
    render_template_string,
    request,
    url_for,
)

from ..auth import login_required, role_required
from ..db import get_db
from ..imports import load_fee_schedule
from ..models import Role
from ..notifications import build_reminder_message, post_fee_reminder
from ..payments import build_refund_request, charge_card, refund_payment
from . import repository
from .fees import compute_late_fee, invoice_total, parse_due_date

invoices_bp = Blueprint("invoices", __name__)

SORT_COLUMNS = {
    "due": "due_date",
    "amount": "amount_cents",
    "created": "created_at",
    "reference": "reference",
}


@invoices_bp.route("/")
@role_required("admin", "bursar")
def list_open_invoices():
    status = request.args.get("status", "open")
    sort_column = SORT_COLUMNS.get(request.args.get("sort", "due"), "due_date")
    direction = "DESC" if request.args.get("dir") == "desc" else "ASC"
    rows = repository.list_invoices(status, sort_column, direction)
    return jsonify(invoices=[dict(r) for r in rows])


def _load_owned_invoice(invoice_id):
    invoice = repository.get_invoice_by_id(invoice_id)
    if invoice is None:
        abort(404)
    if g.user["role"] == Role.GUARDIAN.value and invoice["guardian_id"] != g.user["guardian_id"]:
        abort(404)
    return invoice


@invoices_bp.route("/<int:invoice_id>")
@login_required
def invoice_detail(invoice_id):
    invoice = _load_owned_invoice(invoice_id)
    lines = repository.invoice_lines(invoice_id)
    invoice["lines"] = [dict(line) for line in lines]
    invoice["computed_total_cents"] = invoice_total(lines)
    invoice["late_fee_cents"] = compute_late_fee(
        invoice["amount_cents"] - invoice["paid_cents"],
        parse_due_date(invoice["due_date"]),
        date.today(),
        current_app.config["LATE_FEE_RATE"],
    )
    return jsonify(invoice)


@invoices_bp.route("/<int:invoice_id>/receipt")
@login_required
def download_invoice_receipt(invoice_id):
    invoice = repository.get_invoice_by_id(invoice_id)
    if invoice is None:
        abort(404)
    payments = get_db().execute(
        "SELECT amount_cents, gateway_ref, paid_at FROM payments WHERE invoice_id = ?",
        (invoice_id,),
    ).fetchall()
    return jsonify(reference=invoice["reference"], payments=[dict(p) for p in payments])


@invoices_bp.route("/<int:invoice_id>/note", methods=["PUT"])
@role_required("admin", "bursar")
def update_note(invoice_id):
    note = (request.get_json(silent=True) or {}).get("note", "")[:1000]
    repository.save_invoice_note(invoice_id, note)
    return jsonify(status="saved")


@invoices_bp.route("/<int:invoice_id>/note/preview", methods=["POST"])
@role_required("admin", "bursar")
def preview_invoice_note(invoice_id):
    invoice = _load_owned_invoice(invoice_id)
    note = request.form.get("note", "")
    template = f"<h3>Invoice {{{{ reference }}}}</h3><div class='note'>{note}</div>"
    return render_template_string(template, reference=invoice["reference"])


@invoices_bp.route("/<int:invoice_id>/pay", methods=["POST"])
@login_required
def pay_invoice(invoice_id):
    invoice = _load_owned_invoice(invoice_id)
    payload = request.get_json(silent=True) or {}
    balance = invoice["amount_cents"] - invoice["paid_cents"]
    if invoice["status"] != "open" or balance <= 0:
        return jsonify(error="invoice is not payable"), 409
    result = charge_card(
        invoice_id, payload.get("card_number"), payload.get("expiry"), payload.get("cvv"), balance
    )
    if not result.approved:
        return jsonify(error="payment declined", detail=result.message), 402
    repository.record_payment(invoice_id, balance, result.gateway_ref)
    return jsonify(status="paid", gateway_ref=result.gateway_ref)


@invoices_bp.route("/payment-complete")
@login_required
def payment_complete():
    return_to = request.args.get("return_to")
    if return_to:
        return redirect(return_to)
    return redirect(url_for("students.dashboard"))


@invoices_bp.route("/<int:invoice_id>/refunds", methods=["POST"])
@role_required("admin", "bursar")
def refund(invoice_id):
    payload = request.get_json(silent=True) or {}
    payment = get_db().execute(
        "SELECT gateway_ref, amount_cents FROM payments WHERE invoice_id = ? AND gateway_ref = ?",
        (invoice_id, payload.get("gateway_ref")),
    ).fetchone()
    if payment is None:
        abort(404)
    refund_request = build_refund_request(
        payment["gateway_ref"], invoice_id, payment["amount_cents"], payload.get("reason", "")
    )
    return jsonify(refund_id=refund_payment(refund_request)), 201


@invoices_bp.route("/<int:invoice_id>/remind", methods=["POST"])
@role_required("admin", "bursar")
def send_fee_reminder(invoice_id):
    invoice = repository.get_invoice_by_id(invoice_id)
    if invoice is None or invoice["status"] != "open":
        abort(404)
    guardian = get_db().execute(
        "SELECT id, phone FROM guardians WHERE id = ?", (invoice["guardian_id"],)
    ).fetchone()
    message = build_reminder_message(invoice, guardian)
    delivered = post_fee_reminder(current_app.config["REMINDER_WEBHOOK_URL"], message)
    return jsonify(delivered=delivered)


@invoices_bp.route("/fee-schedule", methods=["POST"])
@role_required("admin", "bursar")
def upload_fee_schedule():
    try:
        fees = load_fee_schedule(request.get_data(as_text=True))
    except (KeyError, InvalidOperation) as exc:
        return jsonify(error=f"invalid fee schedule: {exc}"), 400
    return jsonify(fees={code: str(amount) for code, amount in fees.items()})


@invoices_bp.route("/", methods=["POST"])
@role_required("admin", "bursar")
def create():
    payload = request.get_json(silent=True) or {}
    try:
        amount_cents = int(Decimal(str(payload["amount"])) * 100)
        due = parse_due_date(payload["due_date"])
    except (KeyError, ValueError, InvalidOperation):
        return jsonify(error="amount and due_date are required"), 400
    invoice_id = repository.create_invoice(
        payload["student_id"], payload["guardian_id"], payload["term_code"], amount_cents,
        due.isoformat(),
    )
    return jsonify(id=invoice_id), 201
