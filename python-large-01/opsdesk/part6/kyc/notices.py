"""Borrower notices for identity verification (sent through the transactional mail relay)."""

import logging
import os

import requests

from opsdesk.part6.kyc.models import BorrowerIdentity
from opsdesk.part6.privacy import mask_email
from opsdesk.part6.timefmt import notice_date

logger = logging.getLogger(__name__)

MAIL_RELAY_URL = os.environ.get("OPSDESK_MAIL_RELAY_URL", "http://mail-relay.internal:8025/v1/messages")
MAIL_RELAY_TIMEOUT = 5
TEMPLATE_RECEIVED = "kyc-received"
TEMPLATE_DECIDED = "kyc-decided"


def _send(template: str, identity: BorrowerIdentity, variables: dict) -> bool:
    message = {
        "template": template,
        "to": identity.email,
        "locale": identity.country.lower(),
        "variables": variables,
        "tags": {"borrower_id": str(identity.borrower_id)},
    }
    try:
        response = requests.post(MAIL_RELAY_URL, json=message, timeout=MAIL_RELAY_TIMEOUT)
        response.raise_for_status()
    except requests.RequestException:
        logger.warning("mail relay rejected %s notice for borrower %s", template, identity.borrower_id)
        return False
    return True


def send_verification_notice(identity: BorrowerIdentity) -> bool:
    """Tell the borrower their documents arrived and when to expect a decision."""
    sent = _send(
        TEMPLATE_RECEIVED,
        identity,
        {"first_name": identity.full_name.split(" ", 1)[0], "received_on": notice_date(identity.created_at)},
    )
    if sent:
        logger.info("verification notice sent to %s for borrower %s", mask_email(identity.email), identity.borrower_id)
    return sent


def send_decision_notice(identity: BorrowerIdentity) -> bool:
    """Tell the borrower the outcome of the manual review."""
    return _send(
        TEMPLATE_DECIDED,
        identity,
        {"outcome": identity.status.lower(), "decided_on": notice_date(identity.reviewed_at)},
    )
