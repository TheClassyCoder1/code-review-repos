from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..models import LedgerEntry, Member


async def ledger_balance(session: AsyncSession, member_id: int) -> int:
    total = await session.scalar(
        select(func.coalesce(func.sum(LedgerEntry.points), 0)).where(
            LedgerEntry.member_id == member_id
        )
    )
    return int(total)


EARNING_KINDS = frozenset({"earn", "bonus"})


def is_earning(kind: str) -> bool:
    return kind in EARNING_KINDS


async def credit_member(
    session: AsyncSession, member: Member, points: int, kind: str, reference: str
) -> None:
    member.points_balance = member.points_balance + points
    if is_earning(kind):
        member.lifetime_points = member.lifetime_points + points
    session.add(
        LedgerEntry(member_id=member.id, kind=kind, points=points, reference=reference)
    )


async def balance_drift(session: AsyncSession, member: Member) -> int:
    return member.points_balance - await ledger_balance(session, member.id)


async def post_burn(session: AsyncSession, member_id: int, points: int, reference: str) -> None:
    session.add(LedgerEntry(member_id=member_id, kind="burn", points=-points, reference=reference))
    await session.flush()
    await session.execute(
        update(Member)
        .where(Member.id == member_id)
        .values(points_balance=Member.points_balance - points)
    )
