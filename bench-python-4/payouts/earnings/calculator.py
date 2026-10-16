from decimal import ROUND_HALF_UP, Decimal

from payouts import config

CENT = Decimal("0.01")


def to_money(value):
    return Decimal(value).quantize(CENT, rounding=ROUND_HALF_UP)


def trip_net_earning(trip):
    gross = trip["fare"] + trip["tip"]
    commission = trip["fare"] * config.COMMISSION_RATE
    net = gross - commission - trip["booking_fee"]
    return to_money(net)


def commission_breakdown(trip):
    commission = to_money(trip["fare"] * config.COMMISSION_RATE)
    return {"fare": trip["fare"], "commission": commission, "tip": trip["tip"]}


def weekly_incentive(trip_count):
    for minimum_trips, bonus in config.WEEKLY_INCENTIVE_TIERS:
        if trip_count >= minimum_trips:
            return bonus
    return Decimal("0.00")


def settlement_net_total(gross, adjustments, bonus):
    return to_money(gross + adjustments + bonus)


def is_cashout_allowed(balance):
    return balance >= config.MIN_INSTANT_CASHOUT


def instant_cashout_amount(balance):
    fee = float(balance) * float(config.INSTANT_CASHOUT_FEE_RATE)
    payable = round(float(balance) - fee, 2)
    return {"balance": float(balance), "fee": round(fee, 2), "payable": payable}
