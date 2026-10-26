"""Data query endpoints."""

from fastapi import APIRouter, HTTPException, status

from opsdesk.db import DbSession, PrimaryKey
from opsdesk.part2.data_queries.models import QueryRead, QueryUpdate
from opsdesk.part2.data_queries.service import delete, get, update

router = APIRouter()


@router.put("/{query_id}", response_model=QueryRead)
def update_query(db_session: DbSession, query_id: PrimaryKey, query_in: QueryUpdate):
    """Updates a data query."""
    query = get(db_session=db_session, query_id=query_id)
    if not query:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=[{"msg": "A data query with this id does not exist."}],
        )
    return update(db_session=db_session, query=query, query_in=query_in)


@router.delete("/{query_id}", response_model=None)
def delete_query(db_session: DbSession, query_id: PrimaryKey):
    """Deletes a data query, returning only an HTTP 200 OK if successful."""
    query = get(db_session=db_session, query_id=query_id)
    if not query:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=[{"msg": "A data query with this id does not exist."}],
        )
    delete(db_session=db_session, query_id=query_id)
