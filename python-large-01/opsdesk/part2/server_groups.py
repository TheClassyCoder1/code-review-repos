"""Per-tenant (project) placement groups for the dedicated scoring workers."""

from sqlalchemy import JSON, String
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base
from opsdesk.part2 import api_db_api


class InstanceGroup(Base):
    __tablename__ = "scoring_instance_group"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    project_id: Mapped[str] = mapped_column(String(64), index=True)
    policy: Mapped[str] = mapped_column(String(32), default="anti-affinity")
    members: Mapped[list] = mapped_column(JSON, default=list)


def _instance_group_get_query(context):
    return context.session.query(InstanceGroup).order_by(InstanceGroup.id)


class InstanceGroupList(list):

    @staticmethod
    @api_db_api.context_manager.reader
    def _get_from_db(context, project_id=None):
        query = _instance_group_get_query(context)
        if project_id is not None:
            query = query.filter_by(project_id=project_id)
        return query.all()

    @classmethod
    def get_by_project_id(cls, context, project_id):
        return cls(cls._get_from_db(context, project_id=project_id))

    @classmethod
    def get_all(cls, context):
        return cls(cls._get_from_db(context))
