import httpx
from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..members.ledger import credit_member, post_burn
from ..models import Member, Partner, PartnerNotification, Redemption, Reward
from ..partners.client import confirm_redemption
from ..partners.notify import notification_payload, notify_partner_best_effort


async def redeem_reward(
    session: AsyncSession, client: httpx.AsyncClient, member_id: int, reward_id: int
) -> Redemption:
    async with session.begin():
        member = await session.scalar(
            select(Member).where(Member.id == member_id).with_for_update()
        )
        if member is None or member.status != "active":
            raise HTTPException(status_code=404, detail="member not found")
        reward = await session.get(Reward, reward_id)
        if reward is None or not reward.active:
            raise HTTPException(status_code=404, detail="reward not found")
        if member.points_balance < reward.cost_points:
            raise HTTPException(status_code=409, detail="insufficient points")
        await post_burn(session, member.id, reward.cost_points, f"reward:{reward.id}")
        redemption = Redemption(member_id=member.id, reward_id=reward.id, points=reward.cost_points)
        session.add(redemption)
        await session.flush()
        notification = PartnerNotification(
            partner_id=reward.partner_id, redemption_id=redemption.id, status="pending"
        )
        session.add(notification)
        await session.flush()
        partner_code = await session.scalar(select(Partner.code).where(Partner.id == reward.partner_id))
    await notify_partner_best_effort(
        client, notification.id, partner_code, notification_payload(redemption)
    )
    return redemption


async def cancel_redemption(session: AsyncSession, member_id: int, redemption_id: int) -> Redemption:
    redemption = await session.get(Redemption, redemption_id, with_for_update=True)
    if redemption is None or redemption.member_id != member_id:
        raise HTTPException(status_code=404, detail="redemption not found")
    if redemption.status != "pending":
        raise HTTPException(status_code=409, detail=f"redemption is {redemption.status}")
    member = await session.get(Member, member_id, with_for_update=True)
    redemption.status = "cancelled"
    await credit_member(session, member, redemption.points, "refund", f"redemption:{redemption.id}")
    await session.commit()
    return redemption


async def fulfil_redemption(session: AsyncSession, redemption_id: int) -> str:
    redemption = await session.get(Redemption, redemption_id)
    if redemption is None or redemption.status != "pending":
        raise HTTPException(status_code=404, detail="no pending redemption")
    reward = await session.get(Reward, redemption.reward_id)
    partner = await session.get(Partner, reward.partner_id)
    code = await confirm_redemption(partner.code, redemption.id, redemption.points)
    redemption.status = "fulfilled"
    await session.commit()
    return code
