"""Q-object builders for the translation workspace's sidebar filters."""

import shlex
from datetime import datetime

from django.db.models import Q
from django.utils.timezone import make_aware

TIME_FORMAT = "%Y%m%d%H%M"


def _parse_range(value: str):
    start, _, end = value.partition("-")
    return (
        make_aware(datetime.strptime(start, TIME_FORMAT)),
        make_aware(datetime.strptime(end, TIME_FORMAT)),
    )


def _time_and_user_filters(locale, time, created_time, review_time, author, reviewer):
    """Filters on translation metadata, applied before any aggregation."""
    filters = []
    if time:
        start, end = _parse_range(time)
        filters.append(
            Q(translation__locale=locale)
            & (Q(translation__date__range=(start, end)) | Q(translation__approved_date__range=(start, end)))
        )
    if created_time:
        filters.append(Q(date_created__range=_parse_range(created_time)))
    if review_time:
        filters.append(Q(translation__locale=locale, translation__approved_date__range=_parse_range(review_time)))
    if author:
        filters.append(Q(translation__locale=locale, translation__user__username__in=author.split(",")))
    if reviewer:
        filters.append(Q(translation__locale=locale, translation__approved_user__username__in=reviewer.split(",")))
    return filters


def _status_filter(project, locale, status):
    statuses = set(status.split(","))
    query = Q()
    active = Q(translation__locale=locale, translation__active=True)
    if "missing" in statuses:
        query |= ~Q(pk__in=_entity_ids_with(locale, active=True))
    if "translated" in statuses:
        query |= Q(active & Q(translation__approved=True))
    if "unreviewed" in statuses:
        query |= Q(translation__locale=locale, translation__approved=False, translation__rejected=False)
    return query or None


def _entity_ids_with(locale, **translation_fields):
    from opsdesk.part4.l10n.models import Translation

    return Translation.objects.filter(locale=locale, **translation_fields).values("entity_id")


def _extra_filter(locale, extra):
    extras = set(extra.split(","))
    query = Q()
    if "rejected" in extras:
        query |= Q(translation__locale=locale, translation__rejected=True)
    return query or None


def get_search_phrases(search: str) -> list[str]:
    """Split a search box value into phrases, honouring double quotes."""
    try:
        phrases = shlex.split(search)
    except ValueError:
        phrases = search.split()
    return [p for p in phrases if p.strip()]
