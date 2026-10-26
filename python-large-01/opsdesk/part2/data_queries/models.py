"""Saved data queries that incident responders run against the lending replicas."""

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import String, Text
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base


class DataQuery(Base):
    __tablename__ = "data_query"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(255), unique=True)
    text: Mapped[str] = mapped_column(Text, default="")


class QueryUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=255)
    text: str | None = Field(default=None, max_length=20000)


class QueryRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    text: str
