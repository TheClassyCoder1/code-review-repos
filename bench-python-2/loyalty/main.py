from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .config import settings
from .members.routes import router as members_router
from .partners.routes import admin_router as partner_admin_router
from .partners.routes import router as partners_router
from .rules.routes import router as rules_router
from .statements.routes import router as statements_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.partner_http = httpx.AsyncClient(
        base_url=settings.partner_api_base,
        timeout=httpx.Timeout(settings.partner_timeout_seconds),
    )
    try:
        yield
    finally:
        await app.state.partner_http.aclose()


app = FastAPI(title="Loyalty Points", lifespan=lifespan)


@app.middleware("http")
async def partner_request_guard(request: Request, call_next):
    if request.url.path.startswith("/partners/"):
        if "x-partner-id" not in request.headers:
            return JSONResponse({"detail": "missing partner id"}, status_code=401)
        if request.method == "POST":
            length = request.headers.get("content-length", "")
            if not length.isdigit() or int(length) > settings.max_webhook_bytes:
                return JSONResponse({"detail": "payload too large"}, status_code=413)
    return await call_next(request)


app.include_router(members_router)
app.include_router(rules_router)
app.include_router(partners_router)
app.include_router(partner_admin_router)
app.include_router(statements_router)


@app.get("/healthz")
async def healthz() -> dict:
    return {"status": "ok"}
