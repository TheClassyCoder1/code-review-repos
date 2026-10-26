"""Console login identities and embeddable saved queries."""

import hashlib
import secrets
from types import SimpleNamespace

from flask_login import UserMixin
from sqlalchemy import JSON, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, scoped_session

from opsdesk.console.models import Organization
from opsdesk.db import Base, SessionLocal

db = SimpleNamespace(session=scoped_session(SessionLocal))


class _QueryProperty:
    def __get__(self, obj, owner):
        return db.session.query(owner)


class User(UserMixin, Base):
    __tablename__ = "console_login"
    query = _QueryProperty()

    id: Mapped[int] = mapped_column(ForeignKey("console_user.id"), primary_key=True)
    org_id: Mapped[int] = mapped_column(index=True)
    password_hash: Mapped[str | None] = mapped_column(String(255), nullable=True)
    api_key: Mapped[str] = mapped_column(String(64), unique=True, default=lambda: secrets.token_urlsafe(30))
    details: Mapped[dict] = mapped_column(JSON, default=dict)

    @classmethod
    def get_by_id_and_org(cls, user_id, org):
        return cls.query.filter(cls.id == int(user_id), cls.org_id == org.id).one()

    @property
    def is_invitation_pending(self) -> bool:
        return bool(self.details.get("is_invitation_pending", False))

    @is_invitation_pending.setter
    def is_invitation_pending(self, value: bool):
        self.details = {**self.details, "is_invitation_pending": bool(value)}

    def hash_password(self, password: str) -> None:
        salt = secrets.token_hex(8)
        digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 390_000)
        self.password_hash = f"pbkdf2_sha256${salt}${digest.hex()}"


class Query(Base):
    __tablename__ = "console_saved_query"
    query = _QueryProperty()

    id: Mapped[int] = mapped_column(primary_key=True)
    org_id: Mapped[int] = mapped_column(index=True)
    api_key: Mapped[str] = mapped_column(String(64), unique=True, default=lambda: secrets.token_urlsafe(30))
    groups: Mapped[dict] = mapped_column(JSON, default=dict)

    @property
    def org(self) -> Organization | None:
        return db.session.get(Organization, self.org_id)


class ApiUser(UserMixin):
    def __init__(self, api_key, org, groups, name=None):
        self.id, self.org, self.group_ids, self.name = api_key, org, groups, name
