"""Entity listing for the translation workspace."""

from re import escape
from typing import cast

from django.contrib.auth.models import User
from django.db.models import Q, QuerySet

from opsdesk.part4.l10n.filters import _extra_filter, _status_filter, _time_and_user_filters, get_search_phrases
from opsdesk.part4.l10n.models import Entity, Locale, Project, ProjectQuerySet, Resource


def get_entities_for_project_locale(
    user: User,
    project: Project,
    locale: Locale,
    paths: list[str] | None = None,
    status: str | None = None,
    tag: str | None = None,
    search: str | None = None,
    extra: str | None = None,
    search_identifiers: bool = False,
    search_exclude_source_strings: bool = False,
    search_rejected_translations: bool = False,
    search_match_case: bool = False,
    search_match_whole_word: bool = False,
    time: str | None = None,
    created_time: str | None = None,
    author: str | None = None,
    review_time: str | None = None,
    reviewer: str | None = None,
) -> QuerySet[Entity]:
    """Get project entities with locale translations."""

    entities = Entity.objects.all()

    pre_filter = Q(
        *_time_and_user_filters(
            locale,
            time,
            created_time,
            review_time,
            author,
            reviewer,
        )
    )
    if pre_filter:
        # Time & user filters have to be applied before the aggregation
        # (with_status_counts) and the status & extra filters to avoid
        # unnecessary joins causing performance and logic issues.
        entities = entities.filter(pk__in=Entity.objects.filter(pre_filter))

    entities = entities.filter(
        resource__translatedresources__locale=locale,
        resource__project__disabled=False,
        obsolete=False,
    )

    if project.slug == "all-projects":
        visible_projects = cast(ProjectQuerySet, Project.objects).visible_for(user)
        entities = entities.filter(
            resource__project__system_project=False,
            resource__project__in=visible_projects,
        )
    else:
        entities = entities.filter(resource__project=project)

    if paths:
        entities = entities.filter(resource__path__in=paths)

    post_filter = Q()

    if status and (status_query := _status_filter(project, locale, status)):
        post_filter &= status_query

    if extra and (extra_query := _extra_filter(locale, extra)):
        post_filter &= extra_query

    if tag:
        post_filter &= Q(resource__tag__slug__in=tag.split(","))

    if post_filter:
        entities = entities.filter(post_filter)
        if tag:
            # only tag needs `distinct` as it traverses m2m fields
            entities = entities.distinct()

    # Filter by search parameters
    if search:
        search_list = get_search_phrases(search)

        # Modify query based on case & match sensitivity filters
        i = "" if search_match_case else "i"
        y = r"\y" if search_match_whole_word else ""

        # Use regex to ignore context identifiers by default
        r = "" if search_identifiers else "=.*"
        o = "" if search_identifiers else ".*"

        translation_filters = (
            (
                Q(
                    Q(resource__format=Resource.Format.FLUENT)
                    & (
                        Q(
                            **{
                                f"translation__string__{i}regex": rf"{r}{y}{escape(s)}{y}{o}"
                            }
                        )
                    )
                )
                | Q(
                    ~Q(resource__format=Resource.Format.FLUENT)
                    & Q(**{f"translation__string__{i}regex": rf"{y}{escape(s)}{y}"})
                )
            )
            & Q(translation__locale=locale)
            & (Q() if search_rejected_translations else Q(translation__rejected=False))
            for s in search_list
        )

        translation_matches = entities.filter(*translation_filters).values_list(
            "id", flat=True
        )

        # Search in source strings
        entity_filters = (
            (
                Q(pk__in=[])  # Ensures that no source strings are returned
                if search_exclude_source_strings
                else (
                    Q(
                        Q(resource__format=Resource.Format.FLUENT)
                        & (Q(**{f"string__{i}regex": rf"{r}{y}{escape(s)}{y}{o}"}))
                    )
                    | Q(
                        ~Q(resource__format=Resource.Format.FLUENT)
                        & Q(**{f"string__{i}regex": rf"{y}{escape(s)}{y}"})
                    )
                )
            )
            | (
                Q(**{f"key__{i}regex": rf"{y}{escape(s)}{y}"})
                if search_identifiers
                else Q()
            )
            for s in search_list
        )

        entity_matches = entities.filter(*entity_filters).values_list("id", flat=True)

        entities = Entity.objects.filter(
            pk__in=set(list(translation_matches) + list(entity_matches))
        )

    order_fields: tuple[str, ...] = ("resource__order", "order")
    if project.slug == "all-projects":
        order_fields = ("resource__project__name",) + order_fields

    return entities.order_by(*order_fields)
