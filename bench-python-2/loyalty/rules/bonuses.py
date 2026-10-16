from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..config import settings
from ..models import LedgerEntry, Member

REFERRAL_TIER_FACTOR = {"member": 1, "silver": 1, "gold": 2, "platinum": 3}


async def claim_signup_bonus(session: AsyncSession, member: Member) -> bool:
    already_claimed = await session.scalar(
        select(LedgerEntry.id).where(
            LedgerEntry.member_id == member.id,
            LedgerEntry.kind == "bonus",
            LedgerEntry.reference == "signup",
        )
    )
    if already_claimed is not None:
        return False
    points = settings.signup_bonus_points
    session.add(LedgerEntry(member_id=member.id, kind="bonus", points=points, reference="signup"))
    await session.execute(
        update(Member)
        .where(Member.id == member.id)
        .values(
            points_balance=Member.points_balance + points,
            lifetime_points=Member.lifetime_points + points,
        )
    )
    await session.commit()
    return True


def bonus_reference(kind: str, member_id: int) -> str:
    return f"{kind}:{member_id}"


def referral_bonus_for(referrer_tier: str, referred_count: int) -> int:
    factor = REFERRAL_TIER_FACTOR.get(referrer_tier, 1)
    if referred_count >= 10:
        factor += 1
    return settings.referral_bonus_points * factor
