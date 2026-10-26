"""Staff users and their organization memberships."""

import hashlib
import secrets

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from opsdesk.db import Base


class OrganizationMembership(Base):
    __tablename__ = "organization_membership"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("staff_user.id"))
    organization_slug: Mapped[str] = mapped_column(String(64), index=True)
    role: Mapped[str] = mapped_column(String(32), default="member")


class StaffUser(Base):
    __tablename__ = "staff_user"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True)
    password: Mapped[str] = mapped_column(String(255), default="")
    session_token: Mapped[str | None] = mapped_column(String(128), index=True, nullable=True)
    memberships: Mapped[list[OrganizationMembership]] = relationship(lazy="selectin")

    def is_owner(self, organization: str) -> bool:
        return any(m.organization_slug == organization and m.role == "owner" for m in self.memberships)

    def set_password(self, password: str) -> None:
        if len(password) < 12:
            raise ValueError("Password must be at least 12 characters.")
        salt = secrets.token_hex(8)
        digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 390_000)
        self.password, self.session_token = f"pbkdf2_sha256${salt}${digest.hex()}", None


class UserRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str


class AdminPasswordReset(BaseModel):
    new_password: str = Field(min_length=12, max_length=256)
