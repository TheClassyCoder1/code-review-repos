"""Read models for the collections desk, mirroring billing-service's /api/v1/billing responses."""

import datetime
from decimal import Decimal

from pydantic import BaseModel, Field

AGING_BUCKETS = ("1-30", "31-60", "61-90", "90+")
OPEN_STATUSES = frozenset({"ISSUED", "OVERDUE", "DISPUTED"})


class InvoiceLine(BaseModel):
    position: int
    kind: str
    description: str
    quantity: int
    unitAmount: Decimal
    lineTotal: Decimal


class Invoice(BaseModel):
    id: int
    invoiceNumber: str
    accountId: int
    loanId: int | None = None
    status: str
    currency: str
    issueDate: datetime.date | None = None
    dueDate: datetime.date
    totalAmount: Decimal
    paidAmount: Decimal
    outstandingAmount: Decimal
    lateFeeAmount: Decimal = Decimal("0")
    reminderCount: int = 0
    lines: list[InvoiceLine] = Field(default_factory=list)

    @property
    def is_open(self) -> bool:
        return self.status in OPEN_STATUSES

    def days_overdue(self, today: datetime.date) -> int:
        if not self.is_open or self.dueDate >= today:
            return 0
        return (today - self.dueDate).days


class AgingBucket(BaseModel):
    invoices: int = 0
    outstanding: Decimal = Decimal("0")


class AgingReport(BaseModel):
    currency: str
    buckets: dict[str, AgingBucket]

    @property
    def total_outstanding(self) -> Decimal:
        return sum((bucket.outstanding for bucket in self.buckets.values()), Decimal("0"))

    @property
    def total_invoices(self) -> int:
        return sum(bucket.invoices for bucket in self.buckets.values())

    def share(self, bucket: str) -> Decimal:
        """Fraction of the outstanding amount that sits in ``bucket``, to two decimals."""
        total = self.total_outstanding
        if total == 0 or bucket not in self.buckets:
            return Decimal("0")
        return (self.buckets[bucket].outstanding / total).quantize(Decimal("0.01"))


class TopAccount(BaseModel):
    account_id: int
    overdue_invoices: int
    overdue_amount: Decimal
    oldest_due_date: datetime.date


class DunningRun(BaseModel):
    markedOverdue: int
    lateFeesApplied: int
    remindersSent: int


class AccountCollectionsSummary(BaseModel):
    """What an agent sees when opening one account on the collections desk."""

    account_id: int
    open_invoices: int
    overdue_invoices: int
    disputed_invoices: int
    outstanding_by_currency: dict[str, Decimal]
    oldest_overdue_days: int
    reminders_sent: int
    next_action: str
