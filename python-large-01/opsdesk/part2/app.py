"""Data-query and contact admin API (`uvicorn opsdesk.part2.app:app`).

Staff authentication is declared on the application itself, so every included
router is covered and endpoints do not repeat it.
"""

from fastapi import Depends, FastAPI

from opsdesk.db import get_current_user
from opsdesk.part2.data_queries.views import router as query_router
from opsdesk.part2.individuals.views import router as individual_router

app = FastAPI(title="opsdesk-data", dependencies=[Depends(get_current_user)])
app.include_router(query_router, prefix="/api/admin/data/queries", tags=["data"])
app.include_router(individual_router, prefix="/api/admin/individuals", tags=["individuals"])
