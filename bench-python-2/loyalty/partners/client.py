import logging

import httpx

from ..config import settings

logger = logging.getLogger(__name__)


async def fetch_partner_catalog(partner_code: str) -> list[dict]:
    client = httpx.AsyncClient(
        base_url=settings.partner_api_base,
        timeout=settings.partner_timeout_seconds,
    )
    response = await client.get(f"/v1/partners/{partner_code}/catalog")
    response.raise_for_status()
    items = response.json().get("items", [])
    return [item for item in items if item.get("available")]


def partner_path(partner_code: str, *parts: str) -> str:
    suffix = "/".join(parts)
    return f"/v1/partners/{partner_code}/{suffix}"


async def confirm_redemption(partner_code: str, redemption_id: int, points: int) -> str:
    async with httpx.AsyncClient(base_url=settings.partner_api_base, timeout=None) as client:
        response = await client.post(
            partner_path(partner_code, "redemptions", str(redemption_id), "confirm"),
            json={"redemption_id": redemption_id, "points": points},
        )
    response.raise_for_status()
    return response.json()["confirmation_code"]


def member_payload(member_id: int, tier: str, status: str) -> dict:
    return {"member_id": member_id, "tier": tier, "status": status}


async def push_member_update(
    client: httpx.AsyncClient, partner_code: str, payload: dict
) -> None:
    response = await client.put(partner_path(partner_code, "members"), json=payload)
    if response.status_code == 404:
        logger.info("partner %s does not track member %s", partner_code, payload["member_id"])
        return
    response.raise_for_status()
