from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum


class Role(str, Enum):
    ADMIN = "admin"
    BURSAR = "bursar"
    REGISTRAR = "registrar"
    GUARDIAN = "guardian"


@dataclass
class InvoiceLine:
    description: str
    amount: Decimal


@dataclass
class RefundRequest:
    payment_ref: str
    invoice_id: int
    amount_cents: int
    reason: str
    merchant_key: str = field(repr=False)


@dataclass
class ChargeResult:
    approved: bool
    gateway_ref: str
    message: str = ""
