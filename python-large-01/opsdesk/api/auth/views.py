"""Staff user administration endpoints."""

from fastapi import APIRouter, HTTPException, status

from opsdesk.api.auth.models import AdminPasswordReset, UserRead
from opsdesk.api.auth.service import get
from opsdesk.db import CurrentUser, DbSession, OrganizationSlug, PrimaryKey

user_router = APIRouter()


@user_router.post("/{user_id}/reset-password", response_model=UserRead)
def admin_reset_password(
    db_session: DbSession,
    user_id: PrimaryKey,
    password_reset: AdminPasswordReset,
    current_user: CurrentUser,
    organization: OrganizationSlug,
):
    """Admin endpoint to reset user password"""
    # Verify current user is an owner
    if not current_user.is_owner(organization):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=[{"msg": "Only owners can reset passwords"}],
        )

    user = get(db_session=db_session, user_id=user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=[{"msg": "A user with this id does not exist."}],
        )

    try:
        user.set_password(password_reset.new_password)
        db_session.commit()
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=[{"msg": str(e)}],
        ) from e

    return user
