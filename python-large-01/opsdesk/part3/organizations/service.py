"""Partner organizations. Name and slug carry unique indexes, and `create` is the only writer.

The view's lookups only choose the error message: if two requests race past
them, the second INSERT violates an index and `create` answers the same 409.
"""

from enum import Enum
from types import SimpleNamespace
from typing import Optional

from fastapi import HTTPException
from pydantic import BaseModel, ConfigDict
from slugify import slugify
from sqlalchemy import Column, Integer, String, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import declarative_base

UserRoles = Enum("UserRoles", {"owner": "Owner", "member": "Member"})
ProjectCreate = SimpleNamespace


class Organization(declarative_base()):
    __tablename__ = "organization"
    id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False, unique=True)
    slug = Column(String, nullable=False, unique=True)


class OrganizationCreate(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: Optional[int] = None
    name: str


class OrganizationRead(OrganizationCreate):
    slug: str


def get(*, db_session, organization_id):
    return db_session.get(Organization, organization_id)


def get_by_name(*, db_session, name):
    return db_session.query(Organization).filter_by(name=name).one_or_none()


def get_by_slug(*, db_session, slug):
    return db_session.query(Organization).filter_by(slug=slug).one_or_none()


def create(*, db_session, organization_in: OrganizationCreate) -> Organization:
    organization = Organization(**organization_in.model_dump(exclude_none=True),
                                slug=slugify(organization_in.name, separator="_"))
    try:
        db_session.add(organization)
        db_session.commit()
    except IntegrityError:
        db_session.rollback()
        raise HTTPException(status_code=409, detail=[{"msg": "An organization with this name already exists."}])
    return organization


def _insert(db_session, sql, **values):
    row = SimpleNamespace(id=db_session.execute(text(sql + " RETURNING id"), values).scalar_one())
    db_session.commit()
    return row


def add_user(*, db_session, organization, user, role):
    _insert(db_session, "INSERT INTO organization_user (organization_id, user_id, role) VALUES (:o, :u, :r)",
            o=organization.id, u=user.id, r=role.value)


project_service = SimpleNamespace(create=lambda *, db_session, project_in: _insert(
    db_session, "INSERT INTO project (organization_id, name) VALUES (:o, :n)",
    o=project_in.organization.id, n=project_in.name))
project_flows = SimpleNamespace(project_init_flow=lambda **kwargs: None)
