from fastapi import APIRouter, Depends, HTTPException, Path, Query, Response
from fastapi.concurrency import run_in_threadpool
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..security import require_member
from .export import open_statement_pdf, render_statement_pdf
from .service import build_statement_summary, list_statement_lines, statement_file_name

router = APIRouter(prefix="/members/{member_id}/statements", tags=["statements"])


def _read_statement(file_name: str) -> bytes:
    with open_statement_pdf(file_name) as handle:
        return handle.read()


@router.get("/{year}/{month}/lines")
async def statement_lines(
    member_id: int,
    year: int = Path(ge=2000, le=2100),
    month: int = Path(ge=1, le=12),
    page: int = Query(1, ge=1),
    page_size: int = Query(100, ge=1, le=500),
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> list[dict]:
    if caller_id != member_id:
        raise HTTPException(status_code=403, detail="forbidden")
    return await list_statement_lines(session, member_id, year, month, page, page_size)


@router.get("/{year}/{month}/pdf")
async def statement_pdf(
    member_id: int,
    year: int = Path(ge=2000, le=2100),
    month: int = Path(ge=1, le=12),
    caller_id: int = Depends(require_member),
    session: AsyncSession = Depends(get_session),
) -> Response:
    if caller_id != member_id:
        raise HTTPException(status_code=403, detail="forbidden")
    summary = await build_statement_summary(session, member_id, year, month)
    file_name = statement_file_name(member_id, year, month)
    await run_in_threadpool(render_statement_pdf, summary, file_name)
    content = await run_in_threadpool(_read_statement, file_name)
    return Response(content, media_type="application/pdf")
