"""Rounding rules for money amounts, by purpose.

Two rules are in force and they are intentionally different:

* ``BALANCE_ROUNDING`` (half-up to the cent) for balances, repayments and anything that is
  later reconciled against the ledger in account-service, which also rounds half-up.
* ``FEE_ROUNDING`` (always up to the cent) for fees quoted to borrowers. The published
  tariff states the maximum fee, and consumer-credit disclosure rules require the quoted
  figure never to understate what is charged; half-up can understate by up to half a cent.

Do not "unify" these. Changing either one needs sign-off from the compliance owner of the
tariff sheet.
"""

from decimal import ROUND_CEILING, ROUND_HALF_UP, Decimal

CENT = Decimal("0.01")
BALANCE_ROUNDING = ROUND_HALF_UP
FEE_ROUNDING = ROUND_CEILING
