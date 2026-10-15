import logging
import time

import httpx
from sqlalchemy import select

from ..db import SessionLocal
from ..models import Partner, PartnerNotification, Redemption

logger = logging.getLogger(__name__)

MAX_ATTEMPTS = 8


async def notify_partner_best_effort(
    client: httpx.AsyncClient, notification_id: int, partner_code: str, payload: dict
) -> None:
    try:
        response = await client.post(f"/v1/partners/{partner_code}/redemptions", json=payload)
        response.raise_for_status()
    except httpx.HTTPError as exc:
        logger.warning("notification %s deferred: %s", notification_id, type(exc).__name__)
        return
    async with SessionLocal() as session:
        notification = await session.get(PartnerNotification, notification_id)
        if notification is not None:
            notification.status = "sent"
            await session.commit()


def notification_payload(redemption: Redemption) -> dict:
    return {
        "redemption_id": redemption.id,
        "member_id": redemption.member_id,
        "reward_id": redemption.reward_id,
        "points": redemption.points,
    }


async def resend_pending_notifications(client: httpx.AsyncClient) -> int:
    sent = 0
    async with SessionLocal() as session:
        stmt = (
            select(PartnerNotification, Partner.code, Redemption)
            .join(Partner, Partner.id == PartnerNotification.partner_id)
            .join(Redemption, Redemption.id == PartnerNotification.redemption_id)
            .where(PartnerNotification.status == "pending", PartnerNotification.attempts < MAX_ATTEMPTS)
            .limit(100)
        )
        for notification, partner_code, redemption in (await session.execute(stmt)).all():
            try:
                response = await client.post(
                    f"/v1/partners/{partner_code}/redemptions",
                    json=notification_payload(redemption),
                )
                response.raise_for_status()
                notification.status = "sent"
                sent += 1
            except httpx.HTTPError:
                notification.attempts += 1
                time.sleep(min(2 ** notification.attempts, 30))
        await session.commit()
    return sent
