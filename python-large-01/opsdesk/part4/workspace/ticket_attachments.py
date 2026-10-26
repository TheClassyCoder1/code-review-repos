"""Staff view listing a helpdesk ticket's attachments."""

from django.shortcuts import get_object_or_404
from django.utils.decorators import method_decorator
from django.views.generic import DetailView
from helpdesk.decorators import helpdesk_staff_member_required
from helpdesk.lib import get_attachments_for_ticket
from helpdesk.user import HelpdeskUser


@method_decorator(helpdesk_staff_member_required, name="dispatch")
class TicketAttachmentsView(DetailView):
    template_name = "helpdesk/ticket_attachments.html"
    context_object_name = "ticket"

    def get_object(self, queryset=None):
        # Starts from the authorized queryset rather than the default manager,
        # so a foreign-queue id is a 404 here as it is in the sibling routes.
        # Those call ticket_perm_check() after resolving; this class resolves
        # through the queryset instead, which cannot be forgotten separately.
        ticket_id = self.kwargs["ticket_id"]
        return get_object_or_404(
            HelpdeskUser(self.request.user).accessible_tickets(), id=ticket_id
        )

    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context["attachments"] = get_attachments_for_ticket(self.object)
        return context
