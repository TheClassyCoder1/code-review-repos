import csv
import hashlib
import io
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Request, UploadFile
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Partner
from ..schemas import TransactionBatch
from ..security import get_partner_by_code, require_admin, verify_partner_signature
from .client import fetch_partner_catalog
from .service import compute_partner_settlement, ingest_partner_transactions, parse_csv_row

router = APIRouter(prefix="/partners", tags=["partners"])
admin_router = APIRouter(
    prefix="/admin/partners", tags=["partners"], dependencies=[Depends(require_admin)]
)


@router.get("/rewards")
async def partner_rewards(request: Request, session: AsyncSession = Depends(get_session)) -> dict:
    partner_code = request.headers.get("x-partner-id").strip().lower()
    partner = await get_partner_by_code(session, partner_code)
    if partner is None:
        raise HTTPException(status_code=404, detail="unknown partner")
    items = await fetch_partner_catalog(partner.code)
    return {"partner": partner.code, "items": items}


def batch_canonical_form(batch: TransactionBatch) -> str:
    return "|".join(f"{t.member_id}:{t.amount}:{t.reference}" for t in batch.transactions)


def verify_batch_checksum(batch: TransactionBatch) -> None:
    computed = hashlib.sha256(batch_canonical_form(batch).encode()).hexdigest()
    if computed != batch.checksum.lower():
        raise HTTPException(status_code=422, detail="checksum does not match transactions")


@router.post("/webhooks/transactions", status_code=202)
async def receive_transactions(
    batch: TransactionBatch,
    partner: Partner = Depends(verify_partner_signature),
    session: AsyncSession = Depends(get_session),
) -> dict:
    verify_batch_checksum(batch)
    return await ingest_partner_transactions(session, partner, batch.transactions)


async def _partner_or_404(session: AsyncSession, partner_code: str) -> Partner:
    partner = await get_partner_by_code(session, partner_code.lower())
    if partner is None:
        raise HTTPException(status_code=404, detail="unknown partner")
    return partner


@admin_router.post("/{partner_code}/batches", status_code=202)
async def upload_partner_batch(
    partner_code: str, file: UploadFile, session: AsyncSession = Depends(get_session)
) -> dict:
    partner = await _partner_or_404(session, partner_code)
    raw = await file.read()
    reader = csv.DictReader(io.StringIO(raw.decode("utf-8")))
    try:
        requests = [parse_csv_row(row) for row in reader]
    except (KeyError, ValueError) as exc:
        raise HTTPException(status_code=422, detail="malformed batch file") from exc
    return await ingest_partner_transactions(session, partner, requests)


@admin_router.get("/{partner_code}/settlement")
async def partner_settlement(
    partner_code: str,
    period_start: date,
    period_end: date,
    session: AsyncSession = Depends(get_session),
) -> dict:
    if period_end <= period_start:
        raise HTTPException(status_code=400, detail="period_end must be after period_start")
    partner = await _partner_or_404(session, partner_code)
    return await compute_partner_settlement(session, partner, period_start, period_end)
