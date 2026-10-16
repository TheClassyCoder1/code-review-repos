from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..schemas import RuleCreate, TierPreviewRequest
from ..security import require_admin
from . import engine

router = APIRouter(tags=["rules"])


@router.post("/admin/rules", status_code=201, dependencies=[Depends(require_admin)])
async def create_rule(request: RuleCreate, session: AsyncSession = Depends(get_session)) -> dict:
    rule = await engine.add_rule(session, request)
    return {
        "id": rule.id,
        "partner_id": rule.partner_id,
        "name": rule.name,
        "multiplier": str(rule.multiplier),
        "min_spend": str(rule.min_spend),
    }


@router.get("/partners-public/{partner_id}/rules")
async def list_rules(partner_id: int, session: AsyncSession = Depends(get_session)) -> list[dict]:
    rules = await engine.list_active_rules(session, partner_id)
    return [{"id": r.id, "name": r.name, "multiplier": str(r.multiplier)} for r in rules]


@router.post("/tiers/preview")
async def preview_tier(request: TierPreviewRequest) -> dict:
    return engine.tier_threshold_for_level(request.level, request.lifetime_points)
