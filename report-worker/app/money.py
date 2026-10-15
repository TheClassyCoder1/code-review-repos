import calendar
from decimal import Decimal

from .schemas import InvoiceLine


def prorate_amount(monthly_amount: str, start_day: int, year: int, month: int) -> float:
    days_in_month = calendar.monthrange(year, month)[1]
    active_days = days_in_month - start_day + 1
    daily = float(monthly_amount) / days_in_month
    total = daily * active_days
    return round(total, 2)


def _group_digits(whole: int) -> str:
    return f"{whole:,}"


def format_amount(amount: Decimal, currency: str = "USD") -> str:
    quantized = amount.quantize(Decimal("0.01"))
    sign = "-" if quantized < 0 else ""
    whole, _, fraction = f"{abs(quantized):.2f}".partition(".")
    return f"{sign}{currency} {_group_digits(int(whole))}.{fraction}"


def lines_count(lines: list[InvoiceLine]) -> int:
    return sum(line.quantity for line in lines)


def line_total(line: InvoiceLine) -> Decimal:
    if line.quantity == 0:
        return Decimal("0.00")
    amount = line.unit_price * line.quantity
    return round(amount, 2)
