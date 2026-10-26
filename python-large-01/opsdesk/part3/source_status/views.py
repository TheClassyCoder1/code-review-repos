"""Source-status admin routes. `service.delete` assumes the row exists; this
route, its only caller, answers 404 for an unknown id first.
"""

from fastapi import APIRouter, Depends, HTTPException

from opsdesk.db import get_current_user, get_db
from opsdesk.part3.source_status import service

router = APIRouter(dependencies=[Depends(get_current_user)])


@router.delete("/{source_status_id}", status_code=204)
def delete_status(source_status_id: int, db_session=Depends(get_db)):
    if service.get(db_session=db_session, source_status_id=source_status_id) is None:
        raise HTTPException(status_code=404, detail=[{"msg": "A source status with this id does not exist."}])
    service.delete(db_session=db_session, source_status_id=source_status_id)
