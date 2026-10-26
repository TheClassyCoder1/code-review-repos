"""Unit tests for the collections-desk summary logic and report models."""

import datetime
import unittest
from decimal import Decimal

from opsdesk.part7.collections.models import AgingBucket, AgingReport, Invoice
from opsdesk.part7.collections.summary import (
    ACTION_CALL,
    ACTION_ESCALATE,
    ACTION_NONE,
    ACTION_REMIND,
    ACTION_WAIT_FOR_DISPUTE,
    next_action,
    outstanding_by_currency,
    summarise_account,
)

TODAY = datetime.date(2026, 10, 26)


def make_invoice(invoice_id=1, status="OVERDUE", due_days_ago=10, outstanding="100.00", currency="EUR", reminders=0):
    return Invoice(
        id=invoice_id,
        invoiceNumber=f"INV-202610-{invoice_id:06d}",
        accountId=42,
        status=status,
        currency=currency,
        issueDate=TODAY - datetime.timedelta(days=due_days_ago + 30),
        dueDate=TODAY - datetime.timedelta(days=due_days_ago),
        totalAmount=Decimal(outstanding),
        paidAmount=Decimal("0"),
        outstandingAmount=Decimal(outstanding),
        reminderCount=reminders,
    )


class DaysOverdueTests(unittest.TestCase):
    def test_open_invoice_past_due(self):
        self.assertEqual(make_invoice(due_days_ago=12).days_overdue(TODAY), 12)

    def test_not_yet_due(self):
        self.assertEqual(make_invoice(status="ISSUED", due_days_ago=-3).days_overdue(TODAY), 0)

    def test_paid_invoice_is_never_overdue(self):
        self.assertEqual(make_invoice(status="PAID", due_days_ago=40).days_overdue(TODAY), 0)


class NextActionTests(unittest.TestCase):
    def test_no_open_invoices(self):
        self.assertEqual(next_action([make_invoice(status="PAID")], TODAY), ACTION_NONE)

    def test_dispute_blocks_chasing(self):
        invoices = [make_invoice(1, due_days_ago=120), make_invoice(2, status="DISPUTED")]
        self.assertEqual(next_action(invoices, TODAY), ACTION_WAIT_FOR_DISPUTE)

    def test_recent_overdue_gets_reminder(self):
        self.assertEqual(next_action([make_invoice(due_days_ago=5)], TODAY), ACTION_REMIND)

    def test_reminders_exhausted_means_call(self):
        self.assertEqual(next_action([make_invoice(due_days_ago=20, reminders=3)], TODAY), ACTION_CALL)

    def test_over_a_month_means_call(self):
        self.assertEqual(next_action([make_invoice(due_days_ago=45)], TODAY), ACTION_CALL)

    def test_over_ninety_days_escalates(self):
        self.assertEqual(next_action([make_invoice(due_days_ago=91)], TODAY), ACTION_ESCALATE)


class SummaryTests(unittest.TestCase):
    def test_outstanding_is_split_by_currency(self):
        invoices = [
            make_invoice(1, outstanding="10.10"),
            make_invoice(2, outstanding="5.25"),
            make_invoice(3, outstanding="7.00", currency="GBP"),
            make_invoice(4, status="VOID", outstanding="99.00"),
        ]
        self.assertEqual(outstanding_by_currency(invoices), {"EUR": Decimal("15.35"), "GBP": Decimal("7.00")})

    def test_account_summary(self):
        invoices = [make_invoice(1, due_days_ago=8, reminders=1), make_invoice(2, status="ISSUED", due_days_ago=-5)]
        summary = summarise_account(42, invoices, TODAY)
        self.assertEqual(summary.open_invoices, 2)
        self.assertEqual(summary.overdue_invoices, 1)
        self.assertEqual(summary.disputed_invoices, 0)
        self.assertEqual(summary.oldest_overdue_days, 8)
        self.assertEqual(summary.reminders_sent, 1)
        self.assertEqual(summary.next_action, ACTION_REMIND)


class AgingReportTests(unittest.TestCase):
    def test_totals_and_share(self):
        report = AgingReport(
            currency="EUR",
            buckets={
                "1-30": AgingBucket(invoices=3, outstanding=Decimal("300")),
                "31-60": AgingBucket(invoices=1, outstanding=Decimal("100")),
                "61-90": AgingBucket(),
                "90+": AgingBucket(),
            },
        )
        self.assertEqual(report.total_invoices, 4)
        self.assertEqual(report.total_outstanding, Decimal("400"))
        self.assertEqual(report.share("1-30"), Decimal("0.75"))
        self.assertEqual(report.share("90+"), Decimal("0.00"))

    def test_empty_report_share_is_zero(self):
        report = AgingReport(currency="EUR", buckets={})
        self.assertEqual(report.share("1-30"), Decimal("0"))


if __name__ == "__main__":
    unittest.main()
