"""Underwriting document endpoints."""

from fastapi import APIRouter, HTTPException, status

from opsdesk.api.documents.service import delete, get
from opsdesk.db import DbSession, PrimaryKey

router = APIRouter()


@router.delete("/{document_id}", response_model=None)
def delete_document(db_session: DbSession, document_id: PrimaryKey):
    """Delete a document."""
    document = get(db_session=db_session, document_id=document_id)
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=[{"msg": "A document with this id does not exist."}],
        )
    delete(db_session=db_session, document_id=document_id)
