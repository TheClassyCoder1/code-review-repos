"""Reviewer panel for partner integration releases."""

from types import SimpleNamespace

from django.contrib.auth.decorators import permission_required
from django.http import Http404, JsonResponse

from opsdesk.part2.integration_catalog import INTEGRATIONS
from opsdesk.part2.stats import VERSION_ADU_LIMIT, get_average_daily_users_per_version_from_bigquery


def clamp_version_limit(raw) -> int:
    """The warehouse takes the row limit as a literal, so only this int may reach it."""
    try:
        return max(1, min(int(raw), 200))
    except (TypeError, ValueError):
        return VERSION_ADU_LIMIT


@permission_required("backoffice.review_integration", raise_exception=True)
def version_usage(request, guid):
    if guid not in INTEGRATIONS:
        raise Http404("Unknown integration")
    limit = clamp_version_limit(request.GET.get("versions"))
    rows = get_average_daily_users_per_version_from_bigquery(SimpleNamespace(guid=guid), limit=limit)
    return JsonResponse({"versions": [{"version": r["version"], "adu": int(r["adu"])} for r in rows]})
