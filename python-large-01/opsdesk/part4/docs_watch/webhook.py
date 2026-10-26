"""Receives change batches from the docs build pipeline.

The pipeline signs each batch with a shared secret. Every change is checked
against the shape ``process_changes`` relies on before any of the batch is
processed, so a malformed change rejects the whole batch with a 400 instead
of failing half-way through publishing.
"""

import hashlib
import hmac
import json
import logging
import os
import re

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST

from opsdesk.part4.docs_watch.notifications import process_changes

logger = logging.getLogger(__name__)

WEBHOOK_SECRET = os.environ.get("OPSDESK_DOCS_WEBHOOK_SECRET", "")
GITHUB_PR_URL = re.compile(r"^https://github.com/(.+)/pull/(\d+)$")

BROWSER_EVENTS = {"added_stable", "removed_stable", "added_preview"}
REQUIRED_FIELDS = {
    "added_stable": ("path", "browsers"),
    "removed_stable": ("path", "browsers"),
    "added_preview": ("path", "browsers"),
    "added_subfeatures": ("path", "subfeatures"),
    "added_nonnull": ("path", "support_changes"),
    "content_updated": ("page_url", "pr_url"),
}


class InvalidChange(ValueError):
    pass


def _validate_change(index: int, change) -> None:
    if not isinstance(change, dict):
        raise InvalidChange("change {} is not an object".format(index))
    event = change.get("event")
    if event not in REQUIRED_FIELDS:
        raise InvalidChange("change {} has unknown event {!r}".format(index, event))
    for name in REQUIRED_FIELDS[event]:
        if change.get(name) in (None, ""):
            raise InvalidChange("change {} ({}) is missing {}".format(index, event, name))
    if event in BROWSER_EVENTS:
        for entry in change["browsers"]:
            if not isinstance(entry, dict) or not entry.get("browser") or not entry.get("version"):
                raise InvalidChange("change {} has an incomplete browser entry".format(index))
    if event == "added_nonnull":
        if any(not isinstance(i, dict) or not i.get("browser") for i in change["support_changes"]):
            raise InvalidChange("change {} has an incomplete support change".format(index))
    if event == "content_updated":
        if not isinstance(change["pr_url"], str) or not GITHUB_PR_URL.match(change["pr_url"]):
            raise InvalidChange("change {} pr_url is not a GitHub pull request URL".format(index))
        if not isinstance(change["page_url"], str):
            raise InvalidChange("change {} page_url is not a string".format(index))


def validate_changes(changes) -> list:
    if not isinstance(changes, list):
        raise InvalidChange("changes must be a list")
    for index, change in enumerate(changes):
        _validate_change(index, change)
    return changes


def _signature_ok(request) -> bool:
    if not WEBHOOK_SECRET:
        return False
    expected = hmac.new(WEBHOOK_SECRET.encode(), request.body, hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, request.headers.get("X-Docs-Signature", ""))


@csrf_exempt
@require_POST
def docs_changes_webhook(request):
    if not _signature_ok(request):
        return JsonResponse({"error": "bad signature"}, status=403)
    try:
        changes = validate_changes(json.loads(request.body).get("changes"))
    except (json.JSONDecodeError, AttributeError):
        return JsonResponse({"error": "body must be a JSON object"}, status=400)
    except InvalidChange as e:
        return JsonResponse({"error": str(e)}, status=400)

    process_changes(changes)
    logger.info("processed %d docs changes", len(changes))
    return JsonResponse({"processed": len(changes)})
