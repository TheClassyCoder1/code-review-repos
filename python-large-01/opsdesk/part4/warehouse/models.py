"""Saved warehouse queries and the data-source grants that gate them."""

from sqlalchemy import Boolean, ForeignKey, String, Text, select
from sqlalchemy.orm import Mapped, mapped_column, relationship

from opsdesk.db import Base, SessionLocal

view_only = True


class DataSource(Base):
    __tablename__ = "warehouse_data_source"

    id: Mapped[int] = mapped_column(primary_key=True)
    org_id: Mapped[int] = mapped_column(index=True)
    name: Mapped[str] = mapped_column(String(255))
    kind: Mapped[str] = mapped_column(String(32))


class DataSourceGroup(Base):
    __tablename__ = "warehouse_data_source_group"

    id: Mapped[int] = mapped_column(primary_key=True)
    data_source_id: Mapped[int] = mapped_column(ForeignKey("warehouse_data_source.id"), index=True)
    group_id: Mapped[int] = mapped_column(index=True)
    view_only: Mapped[bool] = mapped_column(Boolean, default=False)


class Query(Base):
    __tablename__ = "warehouse_query"

    id: Mapped[int] = mapped_column(primary_key=True)
    org_id: Mapped[int] = mapped_column(index=True)
    data_source_id: Mapped[int] = mapped_column(ForeignKey("warehouse_data_source.id"))
    name: Mapped[str] = mapped_column(String(255))
    query_text: Mapped[str] = mapped_column(Text)
    data_source: Mapped[DataSource] = relationship(lazy="joined")

    @classmethod
    def get_by_id(cls, query_id: int) -> "Query":
        with SessionLocal() as db_session:
            query = db_session.get(cls, query_id)
        if query is None:
            raise LookupError("Query id {} not found.".format(query_id))
        return query


def has_access(data_source: DataSource, user, need_view_only: bool) -> bool:
    """A view-only grant satisfies a view-only need; new SQL needs a full grant."""
    group_ids = list(getattr(user, "group_ids", None) or [])
    if not group_ids:
        return False
    with SessionLocal() as db_session:
        grants = db_session.scalars(
            select(DataSourceGroup).where(
                DataSourceGroup.data_source_id == data_source.id,
                DataSourceGroup.group_id.in_(group_ids),
            )
        ).all()
    if not grants:
        return False
    if need_view_only:
        return True
    return any(not grant.view_only for grant in grants)
