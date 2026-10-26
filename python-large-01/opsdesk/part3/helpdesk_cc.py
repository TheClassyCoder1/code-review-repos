from django.core.exceptions import ValidationError
from django.utils.translation import gettext as _
from helpdesk.models import TicketCC


def subscribe_to_ticket_updates(
    ticket, user_id=None, email=None, can_view=True, can_update=False
):
    if ticket is not None:
        queryset = TicketCC.objects.filter(ticket=ticket, user_id=user_id, email=email)

        # Don't create duplicate entries for subscribers
        if queryset.count() > 0:
            return queryset.first()

        if user_id is None and len(email) < 5:
            raise ValidationError(
                _(
                    "When you add somebody on Cc, you must provide either a User or a valid email. Email: {}"
                ).format(email)
            )

        return ticket.ticketcc_set.create(
            user_id=user_id, email=email, can_view=can_view, can_update=can_update
        )
