from collections import OrderedDict
from datetime import date
from decimal import ROUND_HALF_UP, Decimal

from django.db import connection

CENT = Decimal("0.01")
AGING_BUCKETS = OrderedDict([("current", 0), ("1-30", 30), ("31-60", 60), ("61-90", 90)])


def utilization_report(firm_id, start, end, order_by, direction):
    sql = (
        "SELECT u.id, u.first_name, u.last_name, SUM(t.hours) AS total_hours, "
        "SUM(CASE WHEN t.billable THEN t.hours ELSE 0 END) AS billable_hours "
        "FROM billing_timeentry t JOIN accounts_user u ON u.id = t.attorney_id "
        "WHERE u.firm_id = %s AND t.worked_on BETWEEN %s AND %s "
        f"GROUP BY u.id, u.first_name, u.last_name ORDER BY {order_by} {direction}"
    )
    with connection.cursor() as cursor:
        cursor.execute(sql, [firm_id, start, end])
        columns = [col[0] for col in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]


def _line_amount(entry):
    return (Decimal(entry.hours) * Decimal(entry.rate)).quantize(CENT, rounding=ROUND_HALF_UP)


def billable_total(entries, discount_percent=Decimal("0")):
    if not Decimal("0") <= discount_percent <= Decimal("100"):
        raise ValueError("discount_percent must be between 0 and 100")
    subtotal = sum((_line_amount(e) for e in entries if e.billable), Decimal("0.00"))
    discount = (subtotal * discount_percent / Decimal("100")).quantize(CENT, rounding=ROUND_HALF_UP)
    return subtotal - discount


def realization_rate(billed, worked):
    if worked == 0:
        return Decimal("0")
    return (Decimal(billed) / Decimal(worked)).quantize(Decimal("0.0001"))


def aging_buckets(invoices, today=None):
    today = today or date.today()
    buckets = OrderedDict((name, Decimal("0.00")) for name in AGING_BUCKETS)
    buckets["90+"] = Decimal("0.00")
    for invoice in invoices:
        if invoice.paid:
            continue
        overdue = (today - invoice.due_on).days
        label = "90+"
        for name, limit in AGING_BUCKETS.items():
            if overdue <= limit:
                label = name
                break
        buckets[label] += invoice.total
    return buckets
