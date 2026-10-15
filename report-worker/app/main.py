import sqlite3
from decimal import Decimal

from fastapi import Depends, FastAPI, File, HTTPException, UploadFile
from fastapi.responses import RedirectResponse

from .auth import PortalUser, current_user, require_owner
from .config import resolve_return_url
from .db import fetch_job, get_connection, insert_job
from .limits import BodySizeLimitMiddleware
from .jobs import apply_job_update, job_from_row
from .money import format_amount, line_total
from .schemas import JobUpdate, ReportRequest
from .templates import load_uploaded_spec
from .xml_import import parse_partner_feed

app = FastAPI(title="report-worker")
app.add_middleware(BodySizeLimitMiddleware)


@app.get("/healthz")
def healthz() -> dict:
    return {"status": "ok"}


@app.post("/reports", status_code=202)
def create_report(
    req: ReportRequest,
    user: PortalUser = Depends(current_user),
    conn: sqlite3.Connection = Depends(get_connection),
) -> dict:
    job_id = insert_job(conn, user.user_id, req.template, req.period)
    total = sum((line_total(line) for line in req.lines), Decimal("0"))
    return {"job_id": job_id, "estimated_total": format_amount(total)}


@app.get("/reports/{job_id}")
def get_job_detail(job_id: int, user: PortalUser = Depends(current_user), conn: sqlite3.Connection = Depends(get_connection)) -> dict:
    row = fetch_job(conn, job_id)
    if row is None:
        raise HTTPException(status_code=404, detail="report not found")
    job = require_owner(user, row)
    return {"id": job["id"], "status": job["status"], "title": job["title"], "period": job["period"]}


@app.get("/reports/{job_id}/open")
def open_report(job_id: int, next: str = "/", user: PortalUser = Depends(current_user), conn: sqlite3.Connection = Depends(get_connection)):
    row = fetch_job(conn, job_id)
    if row is None or row["status"] != "done":
        raise HTTPException(status_code=404, detail="report not ready")
    require_owner(user, row)
    return RedirectResponse(url=next, status_code=302)


@app.post("/feeds/usage")
async def import_usage_feed(file: UploadFile = File(...), user: PortalUser = Depends(current_user)) -> dict:
    records = parse_partner_feed(await file.read())
    return {"imported": len(records)}


@app.post("/templates")
async def upload_template(file: UploadFile = File(...), user: PortalUser = Depends(current_user)) -> dict:
    raw = await file.read()
    spec = load_uploaded_spec(raw)
    return {"name": spec["name"], "columns": len(spec["columns"])}


@app.patch("/reports/{job_id}")
def update_job(
    job_id: int,
    update: JobUpdate,
    user: PortalUser = Depends(current_user),
    conn: sqlite3.Connection = Depends(get_connection),
) -> dict:
    row = require_owner(user, fetch_job(conn, job_id))
    job = apply_job_update(job_from_row(row), update)
    conn.execute(
        "UPDATE report_jobs SET title = ?, schedule = ? WHERE id = ?",
        (job.title, job.schedule, job.id),
    )
    conn.commit()
    return {"id": job.id, "title": job.title, "schedule": job.schedule}


@app.get("/return/{destination}")
def back_to_portal(destination: str, user: PortalUser = Depends(current_user)):
    target = resolve_return_url(destination)
    return RedirectResponse(url=target, status_code=303)
