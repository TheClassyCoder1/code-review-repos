"""Collections desk endpoints. All data comes from billing-service; nothing is stored locally."""

import datetime
import logging

from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel

from opsdesk.db import CurrentUser, PrimaryKey
from opsdesk.part7.collections.client import BillingRequestRejected, BillingUnavailable, billing_client
from opsdesk.part7.collections.models import (
    AccountCollectionsSummary,
    AgingReport,
    DunningRun,
    Invoice,
    TopAccount,
)
from opsdesk.part7.collections.summary import summarise_account

logger = logging.getLogger(__name__)

router = APIRouter()

SUPPORTED_CURRENCIES = ("EUR", "GBP", "USD", "INR")


class DisputeBody(BaseModel):
    reason: str


class ResolveBody(BaseModel):
    upheld: bool


def _translate(exc: Exception) -> HTTPException:
    if isinstance(exc, BillingRequestRejected):
        return HTTPException(status_code=exc.status_code, detail=[{"msg": str(exc), "code": exc.code}])
    return HTTPException(
        status_code=status.HTTP_502_BAD_GATEWAY,
        detail=[{"msg": "billing-service is unavailable, try again shortly."}],
    )


@router.get("/aging", response_model=AgingReport)
def aging_report(current_user: CurrentUser, currency: str = Query("EUR")):
    currency = currency.strip().upper()
    if currency not in SUPPORTED_CURRENCIES:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=[{"msg": "unsupported currency"}])
    try:
        return billing_client.aging(currency)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc


@router.get("/top-accounts", response_model=list[TopAccount])
def top_accounts(current_user: CurrentUser, limit: int = Query(20, ge=1, le=200)):
    try:
        return billing_client.top_accounts(limit)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc


@router.get("/accounts/{account_id}", response_model=AccountCollectionsSummary)
def account_summary(current_user: CurrentUser, account_id: PrimaryKey):
    try:
        invoices = billing_client.account_invoices(account_id)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc
    return summarise_account(account_id, invoices, datetime.date.today())


@router.get("/accounts/{account_id}/invoices", response_model=list[Invoice])
def account_invoices(current_user: CurrentUser, account_id: PrimaryKey, open_only: bool = Query(False)):
    try:
        invoices = billing_client.account_invoices(account_id)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc
    if open_only:
        invoices = [invoice for invoice in invoices if invoice.is_open]
    return invoices


@router.post("/invoices/{invoice_id}/dispute", response_model=Invoice)
def dispute_invoice(current_user: CurrentUser, invoice_id: PrimaryKey, body: DisputeBody):
    reason = body.reason.strip()
    if not reason:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=[{"msg": "a reason is required"}])
    try:
        invoice = billing_client.dispute(invoice_id, reason)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc
    logger.info("agent %s opened a dispute on invoice %s", current_user.id, invoice_id)
    return invoice


@router.post("/invoices/{invoice_id}/dispute/resolve", response_model=Invoice)
def resolve_dispute(current_user: CurrentUser, invoice_id: PrimaryKey, body: ResolveBody):
    try:
        invoice = billing_client.resolve_dispute(invoice_id, body.upheld)
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc
    logger.info("agent %s resolved dispute on invoice %s (upheld=%s)", current_user.id, invoice_id, body.upheld)
    return invoice


@router.post("/dunning/run", response_model=DunningRun)
def run_dunning(current_user: CurrentUser):
    if not getattr(current_user, "is_admin", False):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=[{"msg": "admins only"}])
    try:
        result = billing_client.run_dunning()
    except (BillingUnavailable, BillingRequestRejected) as exc:
        raise _translate(exc) from exc
    logger.info("agent %s triggered a manual dunning run", current_user.id)
    return result
