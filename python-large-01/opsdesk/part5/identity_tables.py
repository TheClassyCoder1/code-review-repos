"""Operator console tables for identity groups and their members."""

from django.utils.translation import ngettext_lazy
from horizon import tables
from openstack_dashboard import api


class RemoveMembers(tables.DeleteAction):
    @staticmethod
    def action_present(count):
        return ngettext_lazy(
            "Remove User",
            "Remove Users",
            count
        )

    @staticmethod
    def action_past(count):
        return ngettext_lazy(
            "Removed User",
            "Removed Users",
            count
        )

    name ="removeGroupMember"
    policy_rules = (("identity", "identity:remove_user_from_group"),)

    def allowed(self, request, user=None):
        return api.keystone.keystone_can_edit_group()

    def action(self, request, obj_id):
        user_obj = self.table.get_object_by_id(obj_id)
        group_id = self.table.kwargs["group_id"]
        api.keystone.remove_group_user(request, group_id=group_id, user=user_obj.id)
