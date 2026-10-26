"""Timeline endpoint for the staff ticket dashboard."""

from django.http import JsonResponse
from django.utils.html import conditional_escape
from helpdesk.decorators import helpdesk_staff_member_required

from opsdesk.part2.helpdesk_timeline import TicketTimelineQuery


def escape_timeline_headlines(context: dict) -> dict:
    """Escape every event headline before it leaves the server.

    The widget renders `headline` and `text` as HTML; `text` is pre-escaped, but the
    headline joins customer-controlled titles. Escaping here keeps the query helper
    reusable for the CSV export, which needs titles verbatim.
    """
    for event in context["events"]:
        event["text"]["headline"] = str(conditional_escape(event["text"]["headline"]))
    return context


@helpdesk_staff_member_required
def ticket_timeline(request):
    return JsonResponse(escape_timeline_headlines(TicketTimelineQuery(request.user).get_timeline_context()))
