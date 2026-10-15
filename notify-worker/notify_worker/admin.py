import time

from fastapi import APIRouter

from . import db
from .schemas import WebhookTarget

router = APIRouter()


@router.put("/webhooks")
def register_webhook(target: WebhookTarget) -> dict:
    db.save_webhook_target(target)
    return {"partner_id": target.partner_id}


@router.post("/jobs/purge")
def purge_jobs(older_than_days: int = 30) -> dict:
    if older_than_days < 1:
        older_than_days = 1
    cutoff = time.time() - older_than_days * 86400
    removed = db.purge_before(cutoff)
    return {"removed": removed, "older_than_days": older_than_days}
