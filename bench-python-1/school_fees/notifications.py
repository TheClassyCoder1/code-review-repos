import logging

import requests

from .invoices.fees import cents_to_amount

logger = logging.getLogger(__name__)

WEBHOOK_TIMEOUT_SECONDS = 5


def post_fee_reminder(webhook_url, message):
    response = requests.post(webhook_url, json=message, timeout=WEBHOOK_TIMEOUT_SECONDS)
    if response.status_code >= 400:
        logger.warning("fee reminder webhook returned %s", response.status_code)
        return False
    return True


def reminder_channel(guardian):
    if guardian["phone"]:
        return "sms"
    return "email"


def build_reminder_message(invoice, guardian):
    balance = cents_to_amount(invoice["amount_cents"] - invoice["paid_cents"])
    return {
        "guardian_id": guardian["id"],
        "channel": reminder_channel(guardian),
        "template": "fee-reminder",
        "params": {
            "reference": invoice["reference"],
            "balance": f"{balance:.2f}",
            "due_date": invoice["due_date"],
        },
    }
