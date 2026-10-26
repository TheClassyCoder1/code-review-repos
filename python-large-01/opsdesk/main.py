"""ASGI entry point: FastAPI admin API with the console and back-office mounted."""

import os

from a2wsgi import WSGIMiddleware
from fastapi import FastAPI

from opsdesk.api.auth.views import user_router
from opsdesk.api.documents.views import router as document_router
from opsdesk.console import create_console_app
from opsdesk.part6.kyc.views import router as identity_router
from opsdesk.part7.collections.views import router as collections_router

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "opsdesk.backoffice.settings")

from django.core.wsgi import get_wsgi_application  # noqa: E402

app = FastAPI(title="opsdesk")
app.include_router(user_router, prefix="/api/admin/{organization}/users", tags=["users"])
app.include_router(document_router, prefix="/api/admin/documents", tags=["documents"])
app.include_router(identity_router, prefix="/api/onboarding/identity", tags=["identity"])
app.include_router(collections_router, prefix="/api/admin/collections", tags=["collections"])
app.mount("/console", WSGIMiddleware(create_console_app()))
app.mount("/backoffice", WSGIMiddleware(get_wsgi_application()))
