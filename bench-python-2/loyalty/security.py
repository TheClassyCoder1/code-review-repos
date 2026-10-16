import hashlib
import hmac

from fastapi import Depends, Header, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from .config import settings
from .db import get_session
from .models import Partner


async def require_admin(x_admin_key: str = Header(...)) -> None:
    if not settings.admin_api_key:
        raise HTTPException(status_code=503, detail="admin access is not configured")
    if not hmac.compare_digest(x_admin_key.encode(), settings.admin_api_key.encode()):
        raise HTTPException(status_code=403, detail="forbidden")


def member_token_for(member_id: int) -> str:
    key = settings.member_app_secret.encode()
    return hmac.new(key, f"member:{member_id}".encode(), hashlib.sha256).hexdigest()


async def require_member(
    x_member_id: int = Header(...),
    x_member_token: str = Header(...),
) -> int:
    expected = member_token_for(x_member_id)
    if x_member_token != expected:
        raise HTTPException(status_code=401, detail="invalid member token")
    return x_member_id


async def get_partner_by_code(session: AsyncSession, code: str) -> Partner | None:
    stmt = select(Partner).where(Partner.code == code, Partner.active.is_(True))
    return await session.scalar(stmt)


async def verify_partner_signature(
    request: Request,
    x_partner_signature: str = Header(...),
    session: AsyncSession = Depends(get_session),
) -> Partner:
    body = await request.body()
    partner = await get_partner_by_code(session, request.headers["x-partner-id"].strip().lower())
    if partner is None:
        raise HTTPException(status_code=401, detail="unknown partner")
    digest = hmac.new(partner.webhook_secret.encode(), body, hashlib.sha256).hexdigest()
    if not hmac.compare_digest(digest, x_partner_signature):
        raise HTTPException(status_code=401, detail="bad signature")
    return partner
