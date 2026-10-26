"""Console-side organization and user records."""

from flask import g, session
from sqlalchemy import String, select
from sqlalchemy.orm import Mapped, mapped_column
from werkzeug.local import LocalProxy

from opsdesk.db import Base, SessionLocal


class Organization(Base):
    __tablename__ = "console_organization"

    id: Mapped[int] = mapped_column(primary_key=True)
    slug: Mapped[str] = mapped_column(String(64), unique=True)
    google_apps_domains: Mapped[str] = mapped_column(String(1024), default="")

    @property
    def domains(self) -> list[str]:
        return [d.strip().lower() for d in self.google_apps_domains.split(",") if d.strip()]

    @classmethod
    def get_by_slug(cls, slug: str) -> "Organization | None":
        with SessionLocal() as db_session:
            return db_session.scalars(select(cls).where(cls.slug == slug)).first()

    def __str__(self) -> str:
        return self.slug


class ConsoleUser(Base):
    __tablename__ = "console_user"

    id: Mapped[int] = mapped_column(primary_key=True)
    org_id: Mapped[int] = mapped_column(index=True)
    name: Mapped[str] = mapped_column(String(255))
    email: Mapped[str] = mapped_column(String(255), index=True)
    profile_image_url: Mapped[str] = mapped_column(String(512), default="")
    disabled: Mapped[bool] = mapped_column(default=False)


current_org = LocalProxy(lambda: g.get("org") or Organization.get_by_slug(session.get("org_slug") or "default"))
