from fastapi import APIRouter, Depends, HTTPException, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..partners.client import member_payload, push_member_update
from ..redemptions.service import cancel_redemption, fulfil_redemption, redeem_reward
from ..rules.bonuses import claim_signup_bonus
from ..schemas import AdjustmentRequest, EnrollRequest, ProfileUpdate, RedeemRequest, TransferRequest
from ..security import require_admin, require_member
from . import service

router = APIRouter(tags=["members"])


def _same_member(caller_id: int, member_id: int) -> None:
    if caller_id != member_id:
        raise HTTPException(status_code=403, detail="forbidden")


@router.post("/members", status_code=201)
async def enroll(request: EnrollRequest, session: AsyncSession = Depends(get_session)) -> dict:
    member = await service.enroll_member(session, request)
    return {"id": member.id, "tier": member.tier}


@router.get("/members/{member_id}")
async def get_member_profile(
    member_id: int,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    member = await service.get_member_or_404(session, member_id)
    return {
        "id": member.id,
        "full_name": member.full_name,
        "tier": member.tier,
        "points_balance": member.points_balance,
        "lifetime_points": member.lifetime_points,
    }


@router.patch("/members/{member_id}")
async def update_profile(
    member_id: int,
    update: ProfileUpdate,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    member = await service.update_profile(session, member_id, update.full_name)
    return {"id": member.id, "full_name": member.full_name}


@router.get("/members/{member_id}/ledger")
async def member_ledger(
    member_id: int,
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> list[dict]:
    _same_member(caller_id, member_id)
    return await service.list_member_ledger(session, member_id, page, page_size)


@router.get("/members/{member_id}/referral")
async def referral(
    member_id: int,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    return await service.referral_summary(session, member_id)


@router.post("/members/{member_id}/transfers")
async def transfer(
    member_id: int,
    request: TransferRequest,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    balance = await service.transfer_points(session, member_id, request.to_member_id, request.points)
    return {"points_balance": balance}


@router.post("/members/{member_id}/signup-bonus")
async def signup_bonus(
    member_id: int,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    member = await service.get_member_or_404(session, member_id)
    return {"awarded": await claim_signup_bonus(session, member)}


@router.post("/members/{member_id}/redemptions", status_code=201)
async def redeem(
    member_id: int,
    request: RedeemRequest,
    http_request: Request,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    redemption = await redeem_reward(
        session, http_request.app.state.partner_http, member_id, request.reward_id
    )
    return {"id": redemption.id, "points": redemption.points, "status": redemption.status}


@router.delete("/members/{member_id}/redemptions/{redemption_id}")
async def cancel(
    member_id: int,
    redemption_id: int,
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    _same_member(caller_id, member_id)
    redemption = await cancel_redemption(session, member_id, redemption_id)
    return {"id": redemption.id, "status": redemption.status}


@router.post("/admin/members/{member_id}/adjustments", dependencies=[Depends(require_admin)])
async def adjust(
    member_id: int, request: AdjustmentRequest, session: AsyncSession = Depends(get_session)
) -> dict:
    member = await service.apply_points_adjustment(session, member_id, request.points, request.reason)
    return {"id": member.id, "points_balance": member.points_balance}


@router.post("/admin/members/{member_id}/sync/{partner_code}", dependencies=[Depends(require_admin)])
async def sync_to_partner(
    member_id: int,
    partner_code: str,
    http_request: Request,
    session: AsyncSession = Depends(get_session),
) -> dict:
    member = await service.get_member_or_404(session, member_id)
    payload = member_payload(member.id, member.tier, member.status)
    await push_member_update(http_request.app.state.partner_http, partner_code, payload)
    return {"synced": True}


@router.post("/admin/redemptions/{redemption_id}/fulfil", dependencies=[Depends(require_admin)])
async def fulfil(redemption_id: int, session: AsyncSession = Depends(get_session)) -> dict:
    return {"confirmation_code": await fulfil_redemption(session, redemption_id)}
