from datetime import date
from decimal import ROUND_HALF_UP, Decimal

CENT = Decimal("0.01")
LATE_FEE_GRACE_DAYS = 14


def cents_to_amount(cents):
    return (Decimal(cents) / 100).quantize(CENT)


def amount_to_cents(amount):
    return int((Decimal(amount) * 100).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def compute_late_fee(balance_cents, due_date, today, monthly_rate):
    if balance_cents <= 0:
        return 0
    days_late = (today - due_date).days
    if days_late <= LATE_FEE_GRACE_DAYS:
        return 0
    months_late = (days_late + 29) // 30
    fee = cents_to_amount(balance_cents) * Decimal(monthly_rate) * months_late
    return amount_to_cents(fee.quantize(CENT, rounding=ROUND_HALF_UP))


def parse_due_date(value):
    return date.fromisoformat(value)


def is_overdue(invoice, today):
    return invoice["status"] == "open" and parse_due_date(invoice["due_date"]) < today


def invoice_total(lines, discount_percent=0):
    subtotal = sum((Decimal(line["amount_cents"]) for line in lines), Decimal(0))
    if not 0 <= discount_percent <= 100:
        raise ValueError("discount_percent must be between 0 and 100")
    discount = (subtotal * Decimal(discount_percent) / 100).quantize(
        Decimal("1"), rounding=ROUND_HALF_UP
    )
    return int(subtotal - discount)
