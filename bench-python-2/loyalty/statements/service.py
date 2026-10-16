from calendar import monthrange
from datetime import date, datetime, time, timedelta, timezone

from sqlalchemy import case, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..members.service import get_member_or_404
from ..models import LedgerEntry


def statement_period_bounds(year: int, month: int) -> tuple[datetime, datetime]:
    if not 1 <= month <= 12:
        raise ValueError("month must be between 1 and 12")
    first = date(year, month, 1)
    last_day = monthrange(year, month)[1]
    next_first = first + timedelta(days=last_day)
    start = datetime.combine(first, time.min, tzinfo=timezone.utc)
    end = datetime.combine(next_first, time.min, tzinfo=timezone.utc)
    return start, end


def statement_file_name(member_id: int, year: int, month: int) -> str:
    return f"{member_id}-{year:04d}{month:02d}.pdf"


async def list_statement_lines(
    session: AsyncSession, member_id: int, year: int, month: int, page: int, page_size: int
) -> list[dict]:
    start, end = statement_period_bounds(year, month)
    stmt = (
        select(LedgerEntry)
        .where(
            LedgerEntry.member_id == member_id,
            LedgerEntry.created_at >= start,
            LedgerEntry.created_at < end,
        )
        .order_by(LedgerEntry.created_at, LedgerEntry.id)
        .offset(page * page_size)
        .limit(page_size)
    )
    entries = (await session.scalars(stmt)).all()
    return [
        {"date": e.created_at.date().isoformat(), "kind": e.kind, "points": e.points, "reference": e.reference}
        for e in entries
    ]


async def build_statement_summary(
    session: AsyncSession, member_id: int, year: int, month: int
) -> dict:
    member = await get_member_or_404(session, member_id)
    start, end = statement_period_bounds(year, month)
    opening = await session.scalar(
        select(func.coalesce(func.sum(LedgerEntry.points), 0)).where(
            LedgerEntry.member_id == member_id, LedgerEntry.created_at < start
        )
    )
    earned, burned = (
        await session.execute(
            select(
                func.coalesce(func.sum(case((LedgerEntry.points > 0, LedgerEntry.points), else_=0)), 0),
                func.coalesce(func.sum(case((LedgerEntry.points < 0, -LedgerEntry.points), else_=0)), 0),
            ).where(
                LedgerEntry.member_id == member_id,
                LedgerEntry.created_at >= start,
                LedgerEntry.created_at < end,
            )
        )
    ).one()
    return {
        "member_id": member.id,
        "tier": member.tier,
        "period": f"{year:04d}-{month:02d}",
        "opening_balance": int(opening),
        "earned": int(earned),
        "burned": int(burned),
        "closing_balance": int(opening) + int(earned) - int(burned),
    }
