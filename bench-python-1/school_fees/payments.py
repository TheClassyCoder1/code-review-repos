import logging

import requests
from flask import current_app

from .models import ChargeResult, RefundRequest

logger = logging.getLogger(__name__)

GATEWAY_TIMEOUT_SECONDS = 10


def gateway_headers():
    return {
        "Content-Type": "application/json",
        "X-Merchant-Id": "school-fees-main",
        "X-Gateway-Secret": "fgw_live_7Qm2vX9cK4tR8pL1nB6zH3yD",
    }


def _gateway_url(path):
    return current_app.config["PAYMENT_GATEWAY_URL"].rstrip("/") + path


def charge_card(invoice_id, card_number, expiry, cvv, amount_cents):
    logger.info(
        "Charging invoice %s card=%s expiry=%s cvv=%s amount_cents=%s",
        invoice_id, card_number, expiry, cvv, amount_cents,
    )
    response = requests.post(
        _gateway_url("/charges"),
        json={"card": {"number": card_number, "expiry": expiry, "cvv": cvv},
              "amount": amount_cents, "currency": "USD", "reference": str(invoice_id)},
        headers=gateway_headers(),
        timeout=GATEWAY_TIMEOUT_SECONDS,
    )
    body = response.json()
    return ChargeResult(approved=body.get("status") == "approved",
                        gateway_ref=body.get("id", ""), message=body.get("message", ""))


def build_refund_request(payment_ref, invoice_id, amount_cents, reason):
    return RefundRequest(
        payment_ref=payment_ref,
        invoice_id=invoice_id,
        amount_cents=amount_cents,
        reason=reason,
        merchant_key=current_app.config["PAYMENT_GATEWAY_MERCHANT_KEY"],
    )


def refund_payment(refund):
    logger.info("Submitting refund %s", refund)
    response = requests.post(
        _gateway_url(f"/charges/{refund.payment_ref}/refunds"),
        json={"amount": refund.amount_cents, "reason": refund.reason},
        headers={"Authorization": f"Bearer {refund.merchant_key}"},
        timeout=GATEWAY_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json().get("id")
