"""Pure functions that turn billing-service invoices into the collections-desk view."""

import datetime
from collections import defaultdict
from decimal import Decimal

from opsdesk.part7.collections.models import AccountCollectionsSummary, Invoice

ACTION_NONE = "none"
ACTION_WAIT_FOR_DISPUTE = "wait-for-dispute"
ACTION_REMIND = "send-reminder"
ACTION_CALL = "call-borrower"
ACTION_ESCALATE = "escalate-to-recoveries"

CALL_AFTER_DAYS = 30
ESCALATE_AFTER_DAYS = 90
MAX_AUTOMATED_REMINDERS = 3


def outstanding_by_currency(invoices: list[Invoice]) -> dict[str, Decimal]:
    totals: dict[str, Decimal] = defaultdict(lambda: Decimal("0"))
    for invoice in invoices:
        if invoice.is_open:
            totals[invoice.currency] += invoice.outstandingAmount
    return {currency: amount.quantize(Decimal("0.01")) for currency, amount in sorted(totals.items())}


def next_action(invoices: list[Invoice], today: datetime.date) -> str:
    """Pick the single next step an agent should take on the account.

    Disputed invoices block everything else: chasing a borrower while a dispute is open
    breaches the collections policy.
    """
    open_invoices = [invoice for invoice in invoices if invoice.is_open]
    if not open_invoices:
        return ACTION_NONE
    if any(invoice.status == "DISPUTED" for invoice in open_invoices):
        return ACTION_WAIT_FOR_DISPUTE
    oldest = max(invoice.days_overdue(today) for invoice in open_invoices)
    if oldest == 0:
        return ACTION_NONE
    if oldest > ESCALATE_AFTER_DAYS:
        return ACTION_ESCALATE
    if oldest > CALL_AFTER_DAYS:
        return ACTION_CALL
    reminders = max(invoice.reminderCount for invoice in open_invoices)
    if reminders >= MAX_AUTOMATED_REMINDERS:
        return ACTION_CALL
    return ACTION_REMIND


def summarise_account(account_id: int, invoices: list[Invoice], today: datetime.date) -> AccountCollectionsSummary:
    open_invoices = [invoice for invoice in invoices if invoice.is_open]
    return AccountCollectionsSummary(
        account_id=account_id,
        open_invoices=len(open_invoices),
        overdue_invoices=sum(1 for invoice in open_invoices if invoice.status == "OVERDUE"),
        disputed_invoices=sum(1 for invoice in open_invoices if invoice.status == "DISPUTED"),
        outstanding_by_currency=outstanding_by_currency(invoices),
        oldest_overdue_days=max((invoice.days_overdue(today) for invoice in open_invoices), default=0),
        reminders_sent=sum(invoice.reminderCount for invoice in invoices),
        next_action=next_action(invoices, today),
    )
