"""Staff ticket views, overriding the stock django-helpdesk ones."""

from django.contrib import messages
from django.contrib.contenttypes.models import ContentType
from django.core.exceptions import PermissionDenied
from django.db.models import Case, Q, When
from django.http import HttpResponseRedirect
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils.timezone import now
from django.utils.translation import gettext as _
from django.views.decorators.csrf import requires_csrf_token
from helpdesk import settings as helpdesk_settings
from helpdesk.decorators import helpdesk_staff_member_required
from helpdesk.forms import CreateChecklistForm, EditTicketCustomFieldForm, TicketForm
from helpdesk.lib import get_attachments_for_ticket, get_followups_for_ticket
from helpdesk.models import FollowUp, PreSetReply, Ticket, TicketChange
from helpdesk.update_ticket import subscribe_to_ticket_updates, update_ticket
from helpdesk.user import HelpdeskUser
from helpdesk.views.staff import (get_assignable_users, get_form_extra_kwargs,
                                  return_ticketccstring_and_show_subscribe, return_to_ticket, ticket_perm_check)


@helpdesk_staff_member_required
def view_ticket(request, ticket_id):
    ticket = get_object_or_404(Ticket, id=ticket_id)
    try:
        ticket_perm_check(request, ticket)
    except PermissionDenied:
        # Identified by id only. Ticket.__str__ includes the title, so
        # interpolating the object echoed the title of a ticket the user was
        # just refused, turning the denial itself into a disclosure.
        messages.error(
            request,
            _("You don't have permission to view ticket %(ticket_id)s.")
            % {"ticket_id": ticket.id},
        )
        return HttpResponseRedirect(reverse("helpdesk:list"))

    if "take" in request.GET:
        update_ticket(request.user, ticket, owner=request.user.id)
        return return_to_ticket(request.user, ticket)

    if "subscribe" in request.GET:
        # Allow the user to subscribe him/herself to the ticket whilst viewing
        # it.
        show_subscribe = return_ticketccstring_and_show_subscribe(request.user, ticket)[
            1
        ]

        if show_subscribe:
            subscribe_to_ticket_updates(ticket, request.user.id)
            return HttpResponseRedirect(reverse("helpdesk:view", args=[ticket.id]))

    if "close" in request.GET and ticket.status == Ticket.RESOLVED_STATUS:
        if not ticket.assigned_to:
            owner = 0
        else:
            owner = ticket.assigned_to.id

        update_ticket(
            request.user,
            ticket,
            owner=owner,
            comment=_("Accepted resolution and closed ticket"),
        )
        return return_to_ticket(request.user, ticket)

    extra_context_kwargs = get_form_extra_kwargs(request.user)
    form = TicketForm(
        initial={"due_date": ticket.due_date},
        queue_choices=extra_context_kwargs["queues"],
    )
    ticketcc_string, show_subscribe = return_ticketccstring_and_show_subscribe(
        request.user, ticket
    )

    notification_recipients = ticket.get_notification_recipients(user=request.user)

    submitter_userprofile = ticket.get_submitter_userprofile()
    if submitter_userprofile is not None:
        content_type = ContentType.objects.get_for_model(submitter_userprofile)
        submitter_userprofile_url = reverse(
            f"admin:{content_type.app_label}_{content_type.model}_change",
            kwargs={"object_id": submitter_userprofile.id},
        )
    else:
        submitter_userprofile_url = None

    checklist_form = CreateChecklistForm(request.POST or None)
    if checklist_form.is_valid():
        checklist = checklist_form.save(commit=False)
        checklist.ticket = ticket
        checklist.save()

        checklist_template = checklist_form.cleaned_data.get("checklist_template")
        # Add predefined tasks if template has been selected
        if checklist_template:
            checklist.create_tasks_from_template(checklist_template)

        return redirect("helpdesk:edit_ticket_checklist", ticket.id, checklist.id)

    # Both dependency directions expose the id and title of the other ticket, so
    # they are scoped to what this user may open. Without it, opening a ticket of
    # your own disclosed the title of any ticket linked to it, in any queue, with
    # no id manipulation needed.
    reachable = HelpdeskUser(request.user).accessible_tickets()
    # List open tickets on top
    dependencies = (
        ticket.ticketdependency.filter(depends_on__in=reachable)
        .annotate(
            rank=Case(
                When(depends_on__status__in=Ticket.OPEN_STATUSES, then=1), default=2
            )
        )
        .order_by("rank")
    )
    resolves = ticket.depends_on.filter(ticket__in=reachable)

    # add custom fields to further details panel
    customfields_form = EditTicketCustomFieldForm(None, instance=ticket)

    return render(
        request,
        "helpdesk/ticket.html",
        {
            "ticket": ticket,
            "followups": get_followups_for_ticket(ticket),
            "dependencies": dependencies,
            "resolves": resolves,
            "ticket_attachments": get_attachments_for_ticket(ticket),
            "submitter_userprofile_url": submitter_userprofile_url,
            "form": form,
            "preset_replies": PreSetReply.objects.filter(
                Q(queues=ticket.queue) | Q(queues__isnull=True)
            ),
            "ticketcc_string": ticketcc_string,
            "notification_recipients": notification_recipients,
            "SHOW_SUBSCRIBE": show_subscribe,
            "checklist_form": checklist_form,
            "customfields_form": customfields_form,
            "assignable_users": get_assignable_users(
                helpdesk_settings.HELPDESK_STAFF_ONLY_TICKET_OWNERS
            ),
            **extra_context_kwargs,
        },
    )


@helpdesk_staff_member_required
@requires_csrf_token
def hold_ticket(request, ticket_id, unhold=False):
    ticket = get_object_or_404(Ticket, id=ticket_id)
    ticket_perm_check(request, ticket)

    if unhold:
        ticket.on_hold = False
        followup_title = _("Ticket taken off hold")
    else:
        ticket.on_hold = True
        followup_title = _("Ticket placed on hold")

    ticket.save()

    followup = FollowUp.objects.create(
        ticket=ticket,
        title=followup_title,
        date=now(),
        public=True,
        user=request.user,
    )

    TicketChange.objects.create(
        followup=followup,
        field=_("On Hold"),
        old_value=str(not ticket.on_hold),
        new_value=str(ticket.on_hold),
    )

    return HttpResponseRedirect(ticket.get_absolute_url())
