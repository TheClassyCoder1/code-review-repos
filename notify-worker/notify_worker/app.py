import base64
import html
import pickle
import time

from fastapi import Depends, FastAPI, HTTPException, Request

from . import admin, db, templates, tokens
from .deps import require_admin_key, require_internal_key
from .schemas import JobQuery, NotifyJob

app = FastAPI(title="notify-worker")
app.include_router(admin.router, prefix="/admin", dependencies=[Depends(require_admin_key)])
STARTED_AT = time.time()


@app.on_event("startup")
def startup() -> None:
    db.init_db()


@app.get("/health")
def health() -> dict:
    try:
        with db.connect() as conn:
            conn.execute("SELECT 1")
    except Exception:
        raise HTTPException(status_code=503, detail="database unavailable")
    return {"status": "ok", "uptime_seconds": int(time.time() - STARTED_AT)}


@app.post("/jobs", dependencies=[Depends(require_internal_key)])
def create_job(job: NotifyJob) -> dict:
    job_id = db.insert_job(job.model_dump())
    return {"job_id": str(job_id)}


@app.post("/jobs/import", dependencies=[Depends(require_internal_key)])
async def import_jobs(request: Request) -> dict:
    raw = await request.body()
    batch = pickle.loads(base64.b64decode(raw))
    ids = [db.insert_job(NotifyJob(**item).model_dump()) for item in batch]
    return {"imported": len(ids)}


@app.get("/jobs", dependencies=[Depends(require_internal_key)])
def list_jobs(query: JobQuery = Depends()) -> list[dict]:
    return db.list_jobs(query)


@app.get("/jobs/by-recipient", dependencies=[Depends(require_internal_key)])
def jobs_by_recipient(to: str) -> list[dict]:
    return db.jobs_for_recipient(to)


@app.post("/users/{user_id}/unsubscribe-token", dependencies=[Depends(require_internal_key)])
def issue_unsubscribe_token(user_id: int) -> dict:
    return {"token": tokens.unsubscribe_token(user_id)}


@app.post("/jobs/preview", dependencies=[Depends(require_internal_key)])
def preview_job(job: NotifyJob) -> dict:
    body = templates.render_text(job.template, job.context)
    return {"pdf_path": templates.render_pdf(job.model_dump(), f"<p>{html.escape(body)}</p>")}
