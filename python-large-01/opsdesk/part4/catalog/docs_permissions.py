"""Who administers a docs project or organization."""


class AdminPermission:
    @classmethod
    def admins(cls, obj):
        from opsdesk.part4.catalog.docs_projects import Organization
        from opsdesk.part4.catalog.docs_projects import Project

        if isinstance(obj, Project):
            return obj.users.all()

        if isinstance(obj, Organization):
            return obj.owners.all()

        raise ValueError("obj must be an instance of Project or Organization")
