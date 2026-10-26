"""Staff Cc endpoint. `subscribe_to_ticket_updates` checks then inserts, so it
runs under SELECT ... FOR UPDATE on the ticket row: concurrent submits for one
ticket serialize and the second finds the first's row.
"""

from django.db import transaction
from django.http import HttpResponseRedirect
from django.shortcuts import get_object_or_404
from helpdesk.decorators import helpdesk_staff_member_required
from helpdesk.models import Ticket

from opsdesk.part3.helpdesk_cc import subscribe_to_ticket_updates


@helpdesk_staff_member_required
def ticket_cc_add(request, ticket_id):
    with transaction.atomic():
        ticket = get_object_or_404(Ticket.objects.select_for_update(), id=ticket_id)
        subscribe_to_ticket_updates(ticket, user_id=request.POST.get("user"), email=request.POST.get("email"))
    return HttpResponseRedirect(f"../{ticket_id}/")
