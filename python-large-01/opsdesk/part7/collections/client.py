"""HTTP client for billing-service's invoice and collections endpoints."""

import logging
import os

import requests

from opsdesk.part7.collections.models import AgingReport, DunningRun, Invoice, TopAccount

logger = logging.getLogger(__name__)

DEFAULT_BASE_URL = "http://localhost:8092"
CONNECT_TIMEOUT_SECONDS = 2
READ_TIMEOUT_SECONDS = 5
# The dunning pass walks every overdue invoice; give it longer than a normal read.
DUNNING_READ_TIMEOUT_SECONDS = 60


class BillingUnavailable(RuntimeError):
    """billing-service could not be reached or answered with a 5xx."""


class BillingRequestRejected(RuntimeError):
    def __init__(self, status_code: int, code: str, message: str):
        super().__init__(message)
        self.status_code = status_code
        self.code = code


class BillingClient:
    def __init__(self, base_url: str, session: requests.Session | None = None):
        self.base_url = base_url.rstrip("/")
        self._session = session or requests.Session()

    def _request(self, method: str, path: str, *, read_timeout: float = READ_TIMEOUT_SECONDS, **kwargs):
        url = f"{self.base_url}/api/v1/billing{path}"
        try:
            response = self._session.request(method, url, timeout=(CONNECT_TIMEOUT_SECONDS, read_timeout), **kwargs)
        except requests.RequestException as exc:
            logger.warning("billing-service %s %s failed: %s", method, path, type(exc).__name__)
            raise BillingUnavailable(str(exc)) from exc
        if response.status_code >= 500:
            raise BillingUnavailable(f"billing-service answered {response.status_code}")
        if response.status_code >= 400:
            try:
                body = response.json()
            except ValueError:
                body = {}
            raise BillingRequestRejected(
                response.status_code,
                body.get("code", "UNKNOWN"),
                body.get("message", f"billing-service answered {response.status_code}"),
            )
        return response.json()

    def account_invoices(self, account_id: int) -> list[Invoice]:
        return [Invoice.model_validate(item) for item in self._request("GET", f"/accounts/{account_id}/invoices")]

    def invoice(self, invoice_id: int) -> Invoice:
        return Invoice.model_validate(self._request("GET", f"/invoices/{invoice_id}"))

    def aging(self, currency: str) -> AgingReport:
        return AgingReport.model_validate(
            self._request("GET", "/reports/collections/aging", params={"currency": currency})
        )

    def top_accounts(self, limit: int) -> list[TopAccount]:
        rows = self._request("GET", "/reports/collections/top-accounts", params={"limit": limit})
        return [TopAccount.model_validate(row) for row in rows]

    def dispute(self, invoice_id: int, reason: str) -> Invoice:
        return Invoice.model_validate(self._request("POST", f"/invoices/{invoice_id}/dispute", json={"reason": reason}))

    def resolve_dispute(self, invoice_id: int, upheld: bool) -> Invoice:
        return Invoice.model_validate(
            self._request(
                "POST", f"/invoices/{invoice_id}/dispute/resolve", params={"upheld": "true" if upheld else "false"}
            )
        )

    def run_dunning(self) -> DunningRun:
        return DunningRun.model_validate(
            self._request("POST", "/admin/dunning/run", read_timeout=DUNNING_READ_TIMEOUT_SECONDS)
        )


billing_client = BillingClient(os.environ.get("OPSDESK_BILLING_SERVICE_URL", DEFAULT_BASE_URL))
