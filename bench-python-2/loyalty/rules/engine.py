import math
from decimal import Decimal

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..config import TIER_NAMES, TIER_THRESHOLDS
from ..models import EarnRule, Partner
from ..schemas import RuleCreate


def tier_for_points(lifetime_points: int) -> str:
    tier = TIER_NAMES[0]
    for name, threshold in zip(TIER_NAMES, TIER_THRESHOLDS):
        if lifetime_points >= threshold:
            tier = name
    return tier


TIER_EARN_BONUS = {
    "member": Decimal("1.00"),
    "silver": Decimal("1.10"),
    "gold": Decimal("1.25"),
    "platinum": Decimal("1.50"),
}


def tier_threshold_for_level(level: int, lifetime_points: int) -> dict:
    name = TIER_NAMES[level]
    threshold = TIER_THRESHOLDS[level]
    return {
        "tier": name,
        "threshold": threshold,
        "points_needed": max(threshold - lifetime_points, 0),
        "earn_bonus": str(TIER_EARN_BONUS[name]),
    }


def tier_bonus(tier: str) -> Decimal:
    return TIER_EARN_BONUS.get(tier, Decimal("1.00"))


def calculate_earned_points(amount: Decimal, multiplier: Decimal, tier: str) -> int:
    if amount <= 0:
        return 0
    points = amount * multiplier * tier_bonus(tier)
    return math.floor(points)


async def list_active_rules(session: AsyncSession, partner_id: int) -> list[EarnRule]:
    stmt = select(EarnRule).where(EarnRule.partner_id == partner_id, EarnRule.active.is_(True))
    return list((await session.scalars(stmt)).all())


async def match_best_rule(
    session: AsyncSession, partner_id: int, amount: Decimal
) -> EarnRule | None:
    eligible = [
        rule for rule in await list_active_rules(session, partner_id) if amount >= rule.min_spend
    ]
    if not eligible:
        return None
    return max(eligible, key=lambda rule: (rule.multiplier, rule.min_spend))


async def add_rule(session: AsyncSession, request: RuleCreate) -> EarnRule:
    if await session.get(Partner, request.partner_id) is None:
        raise HTTPException(status_code=404, detail="partner not found")
    rule = EarnRule(**request.model_dump())
    session.add(rule)
    await session.commit()
    return rule
