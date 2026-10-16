import logging

import requests

from payouts import config

log = logging.getLogger(__name__)


class BankError(Exception):
    def __init__(self, status, code):
        super().__init__(f"bank error status={status} code={code}")
        self.status = status
        self.code = code


def _headers(idempotency_key=None):
    headers = {"Authorization": f"Bearer {config.BANK_API_KEY}", "Accept": "application/json"}
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key
    return headers


def _raise_for_bank_error(response):
    if response.status_code >= 400:
        try:
            code = response.json().get("error_code", "unknown")
        except ValueError:
            code = "unparseable"
        raise BankError(response.status_code, code)


def create_transfer(instruction, idempotency_key):
    response = requests.post(
        f"{config.BANK_BASE_URL}/transfers",
        json={
            "amount": str(instruction["amount"]),
            "currency": instruction["currency"],
            "beneficiary": {"iban": instruction["iban"], "name": instruction["holder_name"]},
            "reference": instruction["reference"],
        },
        headers=_headers(idempotency_key),
    )
    _raise_for_bank_error(response)
    return response.json()["transfer_id"]


def stream_report_lines(report_url, max_lines=500_000):
    with requests.get(
        report_url, headers=_headers(), stream=True, timeout=config.BANK_TIMEOUT_SECONDS
    ) as response:
        _raise_for_bank_error(response)
        for count, line in enumerate(response.iter_lines(decode_unicode=True)):
            if count >= max_lines:
                raise BankError(413, "report_too_large")
            yield line


def get_transfer_status(transfer_id):
    response = requests.get(
        f"{config.BANK_BASE_URL}/transfers/{transfer_id}",
        headers=_headers(),
        timeout=config.BANK_TIMEOUT_SECONDS,
    )
    if response.status_code == 404:
        return None
    _raise_for_bank_error(response)
    body = response.json()
    return {"status": body["status"], "settled_at": body.get("settled_at")}
