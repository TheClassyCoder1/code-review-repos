"""Request context and listing policy for placement groups.

`_get_from_db` treats a missing project as "every project"; `list_server_groups`
always supplies one unless an admin explicitly asks for all projects.
"""

from dataclasses import dataclass

from opsdesk.part2.server_groups import InstanceGroupList


class Forbidden(Exception):
    pass


@dataclass
class RequestContext:
    project_id: str
    is_admin: bool = False
    session: object = None

    def __post_init__(self):
        if not self.project_id:
            raise Forbidden("request is not scoped to a project")


def list_server_groups(context: RequestContext, all_projects: bool = False):
    if all_projects and context.is_admin:
        return InstanceGroupList.get_all(context)
    return InstanceGroupList.get_by_project_id(context, context.project_id)
