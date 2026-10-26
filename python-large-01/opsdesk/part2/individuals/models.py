"""On-call and escalation contacts for lending operations incidents."""

from pydantic import BaseModel, ConfigDict
from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base


class IndividualContact(Base):
    __tablename__ = "individual_contact"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    email: Mapped[str] = mapped_column(String(255), unique=True)


class IndividualContactRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    email: str
