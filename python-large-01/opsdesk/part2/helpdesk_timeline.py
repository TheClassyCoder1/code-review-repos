"""Ticket query helper that feeds the back-office timeline widget."""

from django.urls import reverse
from django.utils.html import escape
from django.utils.translation import gettext as _
from helpdesk.models import Ticket
from helpdesk.user import HelpdeskUser


class TicketTimelineQuery:
    """Most recently modified tickets a staff member may see."""

    def __init__(self, user, limit=200):
        self.user, self.limit = user, limit

    def get(self):
        tickets = HelpdeskUser(self.user).get_tickets_in_queues().exclude(status=Ticket.DUPLICATE_STATUS)
        return tickets.prefetch_related("followup_set").order_by("-modified")[: self.limit]

    @staticmethod
    def mk_timeline_date(date):
        return {k: getattr(date, k) for k in ("year", "month", "day", "hour", "minute", "second")}

    def get_timeline_context(self):
        events = []

        for ticket in self.get():
            for followup in ticket.followup_set.all():
                event = {
                    "start_date": self.mk_timeline_date(followup.date),
                    "text": {
                        "headline": ticket.title + " - " + followup.title,
                        "text": (
                            (
                                escape(followup.comment)
                                if followup.comment
                                else _("No text")
                            )
                            + '<br/> <a href="{}" class="btn" role="button">{}</a>'.format(
                                reverse(
                                    "helpdesk:view", kwargs={"ticket_id": ticket.pk}
                                ),
                                _("View ticket"),
                            )
                        ),
                    },
                    "group": _("Messages"),
                }
                events.append(event)

        return {
            "events": events,
        }
