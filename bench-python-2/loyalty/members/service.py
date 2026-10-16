import logging

from fastapi import HTTPException
from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..models import LedgerEntry, Member
from ..schemas import EnrollRequest

logger = logging.getLogger(__name__)


async def get_member_or_404(session: AsyncSession, member_id: int) -> Member:
    member = await session.get(Member, member_id)
    if member is None or member.status == "closed":
        raise HTTPException(status_code=404, detail="member not found")
    return member


def _ensure_active(member: Member) -> None:
    if member.status != "active":
        raise HTTPException(status_code=409, detail=f"member is {member.status}")


async def enroll_member(session: AsyncSession, request: EnrollRequest) -> Member:
    email = request.email.strip().lower()
    existing = await session.scalar(select(Member.id).where(Member.email == email))
    if existing is not None:
        raise HTTPException(status_code=409, detail="email already enrolled")
    if request.referred_by_id is not None:
        await get_member_or_404(session, request.referred_by_id)
    member = Member(email=email, full_name=request.full_name.strip(), referred_by_id=request.referred_by_id)
    session.add(member)
    await session.commit()
    logger.info("enrolled member %s", member.id)
    return member


async def update_profile(session: AsyncSession, member_id: int, full_name: str) -> Member:
    member = await get_member_or_404(session, member_id)
    member.full_name = full_name.strip()
    await session.commit()
    return member


async def referral_summary(session: AsyncSession, member_id: int) -> dict:
    member = await get_member_or_404(session, member_id)
    referrer = await session.get(Member, member.referred_by_id)
    referred_count = await session.scalar(
        select(func.count(Member.id)).where(Member.referred_by_id == member.id)
    )
    return {
        "member_id": member.id,
        "referred_by": referrer.full_name,
        "referred_count": referred_count or 0,
    }


def _ledger_entry(member_id: int, kind: str, points: int, reference: str) -> LedgerEntry:
    return LedgerEntry(member_id=member_id, kind=kind, points=points, reference=reference)


async def apply_points_adjustment(
    session: AsyncSession, member_id: int, points: int, reason: str
) -> Member:
    member = await get_member_or_404(session, member_id)
    new_balance = member.points_balance + points
    if new_balance < 0:
        raise HTTPException(status_code=409, detail="adjustment would overdraw the balance")
    member.points_balance = new_balance
    session.add(_ledger_entry(member.id, "adjust", points, reason))
    await session.commit()
    logger.info("adjusted member %s by %s points", member.id, points)
    return member


async def close_member(session: AsyncSession, member_id: int) -> None:
    member = await get_member_or_404(session, member_id)
    member.status = "closed"
    await session.commit()
    logger.info("closed member %s", member.id)


async def transfer_points(
    session: AsyncSession, sender_id: int, recipient_id: int, points: int
) -> int:
    if sender_id == recipient_id:
        raise HTTPException(status_code=400, detail="cannot transfer to yourself")
    sender = await session.get(Member, sender_id, with_for_update=True)
    recipient = await session.get(Member, recipient_id)
    if sender is None or recipient is None:
        raise HTTPException(status_code=404, detail="member not found")
    _ensure_active(sender)
    _ensure_active(recipient)
    if sender.points_balance < points:
        raise HTTPException(status_code=409, detail="insufficient points")
    sender.points_balance -= points
    session.add(_ledger_entry(sender.id, "transfer_out", -points, f"to:{recipient.id}"))
    await session.commit()
    await session.execute(
        update(Member)
        .where(Member.id == recipient.id)
        .values(points_balance=Member.points_balance + points)
    )
    session.add(_ledger_entry(recipient.id, "transfer_in", points, f"from:{sender.id}"))
    await session.commit()
    return sender.points_balance


def _entry_to_dict(entry: LedgerEntry) -> dict:
    return {
        "id": entry.id,
        "kind": entry.kind,
        "points": entry.points,
        "reference": entry.reference,
        "created_at": entry.created_at.isoformat(),
    }


async def list_member_ledger(
    session: AsyncSession, member_id: int, page: int, page_size: int
) -> list[dict]:
    stmt = (
        select(LedgerEntry)
        .where(LedgerEntry.member_id == member_id)
        .order_by(LedgerEntry.created_at.desc(), LedgerEntry.id.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
    )
    entries = (await session.scalars(stmt)).all()
    return [_entry_to_dict(entry) for entry in entries]
