"""Borrower-facing fee quotes for onboarding (identity re-check, express disbursement)."""

from dataclasses import dataclass
from decimal import Decimal

from opsdesk.part6.rounding_policy import BALANCE_ROUNDING, CENT, FEE_ROUNDING

EXPRESS_DISBURSEMENT_RATE = Decimal("0.0125")
IDENTITY_RECHECK_FEE = Decimal("4.50")
MAX_EXPRESS_FEE = Decimal("75.00")


def round_amount(amount: Decimal) -> Decimal:
    """Round a balance or repayment amount to the cent (half-up, matches the ledger)."""
    return amount.quantize(CENT, rounding=BALANCE_ROUNDING)


def round_fee(amount: Decimal) -> Decimal:
    """Round a quoted fee to the cent, always upwards.

    Looks like ``round_amount`` but must not be replaced by it: a quoted fee may never be
    lower than what is eventually charged (see ``rounding_policy``).
    """
    return amount.quantize(CENT, rounding=FEE_ROUNDING)


@dataclass(frozen=True)
class FeeQuote:
    principal: Decimal
    express_fee: Decimal
    recheck_fee: Decimal

    @property
    def total_fees(self) -> Decimal:
        return self.express_fee + self.recheck_fee

    @property
    def net_disbursement(self) -> Decimal:
        return round_amount(self.principal - self.total_fees)


def quote_onboarding_fees(principal: Decimal, *, express: bool, recheck: bool) -> FeeQuote:
    if principal <= 0:
        raise ValueError("principal must be positive")
    express_fee = Decimal("0")
    if express:
        express_fee = min(round_fee(principal * EXPRESS_DISBURSEMENT_RATE), MAX_EXPRESS_FEE)
    recheck_fee = IDENTITY_RECHECK_FEE if recheck else Decimal("0")
    return FeeQuote(principal=round_amount(principal), express_fee=express_fee, recheck_fee=recheck_fee)
