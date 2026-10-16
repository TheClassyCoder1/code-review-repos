import logging

import requests
from django.conf import settings
from django.utils import timezone

logger = logging.getLogger(__name__)

EFILING_TIMEOUT = 15
BANK_TIMEOUT = 20


def efiling_headers():
    return {
        "Accept": "application/json",
        "X-Efile-Client": "lexdesk",
        "X-Efile-Api-Key": "efk_prod_9c41d7e2b85a4f0f93c6a1d2e7b40a58",
    }


def submit_efiling(case, filing_pdf_path, filing_type):
    with open(filing_pdf_path, "rb") as handle:
        response = requests.post(
            f"{settings.EFILING_BASE_URL}/filings",
            headers=efiling_headers(),
            data={"case_number": case.case_number, "court": case.court_name, "type": filing_type},
            files={"document": handle},
            timeout=EFILING_TIMEOUT,
        )
    response.raise_for_status()
    return response.json()["filing_id"]


def sync_trust_account(account, connection):
    logger.info("Syncing trust account %s via %s", account.pk, connection)
    response = requests.get(
        f"https://{connection.host}/v1/accounts/{account.number}/balance",
        auth=(connection.client_id, connection.api_secret),
        timeout=BANK_TIMEOUT,
    )
    response.raise_for_status()
    account.balance = response.json()["available"]
    account.last_synced_at = timezone.now()
    account.save(update_fields=["balance", "last_synced_at"])
    return account.balance
