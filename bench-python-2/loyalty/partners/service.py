import logging
from datetime import date, datetime, time, timezone

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..members.service import get_member_or_404
from ..models import LedgerEntry, Member, Partner
from ..rules.engine import calculate_earned_points, match_best_rule
from ..schemas import EarnRequest

logger = logging.getLogger(__name__)


async def record_earn(session: AsyncSession, partner: Partner, request: EarnRequest) -> int:
    member = await get_member_or_404(session, request.member_id)
    rule = await match_best_rule(session, partner.id, request.amount)
    if rule is None:
        return 0
    points = calculate_earned_points(request.amount, rule.multiplier, member.tier)
    if points == 0:
        return 0
    session.add(
        LedgerEntry(
            member_id=member.id,
            partner_id=partner.id,
            kind="earn",
            points=points,
            reference=request.reference,
        )
    )
    await session.execute(
        update(Member)
        .where(Member.id == member.id)
        .values(
            points_balance=Member.points_balance + points,
            lifetime_points=Member.lifetime_points + points,
        )
    )
    return points


def parse_csv_row(row: dict) -> EarnRequest:
    return EarnRequest(
        member_id=int(row["member_id"]),
        amount=row["amount"],
        reference=row["reference"],
    )


async def ingest_partner_transactions(
    session: AsyncSession, partner: Partner, requests: list[EarnRequest]
) -> dict:
    awarded = 0
    for request in requests:
        try:
            awarded += await record_earn(session, partner, request)
        except Exception:
            logger.warning("partner %s skipped transaction %s", partner.code, request.reference)
    await session.commit()
    return {"status": "accepted", "processed": len(requests), "points_awarded": awarded}


def period_bounds_utc(period_start: date, period_end: date) -> tuple[datetime, datetime]:
    start = datetime.combine(period_start, time.min, tzinfo=timezone.utc)
    end = datetime.combine(period_end, time.min, tzinfo=timezone.utc)
    return start, end


async def compute_partner_settlement(
    session: AsyncSession, partner: Partner, period_start: date, period_end: date
) -> dict:
    start, end = period_bounds_utc(period_start, period_end)
    issued = await session.scalar(
        select(func.coalesce(func.sum(LedgerEntry.points), 0)).where(
            LedgerEntry.partner_id == partner.id,
            LedgerEntry.kind == "earn",
            LedgerEntry.created_at >= start,
            LedgerEntry.created_at < end,
        )
    )
    amount_due = float(issued) * float(partner.settlement_rate)
    return {
        "partner": partner.code,
        "points_issued": int(issued),
        "amount_due": round(amount_due, 2),
        "period_start": period_start.isoformat(),
        "period_end": period_end.isoformat(),
    }
