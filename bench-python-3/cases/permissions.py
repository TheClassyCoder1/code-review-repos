from rest_framework.permissions import SAFE_METHODS, BasePermission


def _matter_for(obj):
    if hasattr(obj, "team"):
        return obj
    return getattr(obj, "case", None)


class HasMatterAccess(BasePermission):
    message = "You are not on the team for this matter."

    def has_object_permission(self, request, view, obj):
        matter = _matter_for(obj)
        if matter is None:
            return True
        if matter.firm_id != request.user.firm_id:
            return False
        if request.method in SAFE_METHODS:
            return True
        return matter.team.filter(pk=request.user.pk).exists()


class IsFirmPartner(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.role == "partner")
