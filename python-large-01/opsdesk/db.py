"""SQLAlchemy session handling and the shared FastAPI dependencies."""

from typing import Annotated

from fastapi import Depends, HTTPException, Path, Request, status
from sqlalchemy import create_engine, select
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from opsdesk.settings import settings

SessionLocal = sessionmaker(bind=create_engine(settings.database_url, pool_pre_ping=True), expire_on_commit=False)


class Base(DeclarativeBase):
    pass


def get_db():
    with SessionLocal() as session:
        yield session


DbSession = Annotated[Session, Depends(get_db)]
PrimaryKey = Annotated[int, Path(gt=0)]
OrganizationSlug = Annotated[str, Path(alias="organization")]


def get_current_user(request: Request, db_session: DbSession):
    from opsdesk.api.auth.models import StaffUser

    token = request.headers.get("Authorization", "").removeprefix("Bearer ").strip()
    user = db_session.scalars(select(StaffUser).where(StaffUser.session_token == token)).first() if token else None
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=[{"msg": "Could not validate credentials"}])
    return user


CurrentUser = Annotated[object, Depends(get_current_user)]
