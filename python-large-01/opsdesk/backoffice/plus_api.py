"""Docs bookmark collection and landing-page survey API."""

from typing import Optional

import requests
from django.conf import settings
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django_ratelimit.decorators import ratelimit
from ninja import Form, Router, Schema
from pydantic import Json

from opsdesk.backoffice.plus_models import MAX_NON_SUBSCRIBED, Bookmark, DocumentURL, LandingPageSurvey, UserProfile

router = Router(tags=["plus"])
Ok = bool


class CollectionUpdateResponse(Schema):
    subscription_limit_reached: bool
    ok: bool = True


class NotOk(Schema):
    error: str
    info: dict = {}


def download_url(url):
    response = requests.get(url, timeout=10)
    response.raise_for_status()
    return response


@router.post(
    "/",
    response={200: CollectionUpdateResponse, 201: CollectionUpdateResponse, 400: NotOk},
    summary="Save or delete a collection item",
)
def save_or_delete_bookmark(
    request,
    url,
    delete: bool = Form(None),
    name: str = Form(None),
    notes: str = Form(None),
):
    absolute_url = f"{settings.BOOKMARKS_BASE_URL}{url}/index.json"
    try:
        documenturl = DocumentURL.objects.get(uri=DocumentURL.normalize_uri(url))
        assert not documenturl.invalid
    except DocumentURL.DoesNotExist:
        response = download_url(absolute_url)
        # Because it's so big, only store certain fields that are used.
        full_metadata = response.json()["doc"]
        metadata = {}
        # Should we so day realize that we want to and need to store more
        # about the remote Yari documents, we'd simply invoke some background
        # processing job that forces a refresh.
        for key in ("title", "mdn_url", "parents"):
            if key in full_metadata:
                metadata[key] = full_metadata[key]

        documenturl = DocumentURL.objects.create(
            uri=DocumentURL.normalize_uri(url),
            absolute_url=absolute_url,
            metadata=metadata,
        )

    bookmark: Optional[Bookmark] = request.user.bookmark_set.filter(
        documenturl=documenturl
    ).first()

    profile: UserProfile = request.auth

    bookmark_count = request.user.bookmark_set.filter(deleted=None).count()
    subscription_limit_reached = False
    if delete:
        if bookmark and not bookmark.deleted:
            bookmark.deleted = timezone.now()
            bookmark.save()
            # Having deleted it's unlikely that the limit will still be reached but check anyway.
            subscription_limit_reached = (bookmark_count - 1) >= MAX_NON_SUBSCRIBED[
                "collection"
            ] and not profile.is_subscriber
        return 200, {
            "subscription_limit_reached": subscription_limit_reached,
            "ok": True,
        }

    subscription_limit_reached = bookmark_count >= MAX_NON_SUBSCRIBED["collection"]

    # Update logic
    if bookmark and not bookmark.deleted:
        if name is not None:
            bookmark.custom_name = name[:500]
        if notes is not None:
            bookmark.notes = notes[:500]
        bookmark.save()
        return 201, {
            "subscription_limit_reached": subscription_limit_reached
            and not profile.is_subscriber,
            "ok": True,
        }

    # Create or undelete. Check limits.
    if not profile.is_subscriber and subscription_limit_reached:
        return 400, {
            "error": "max_subscriptions",
            "info": {"max_allowed": MAX_NON_SUBSCRIBED["collection"]},
        }

    # If found undelete
    if bookmark:
        bookmark.deleted = None
    else:
        # Otherwise, create a brand new entry
        bookmark = request.user.bookmark_set.create(documenturl=documenturl)

    if name is not None:
        bookmark.custom_name = name[:500]
    if notes is not None:
        bookmark.notes = notes[:500]

    bookmark.save()
    subscription_limit_reached = (
        bookmark_count + 1 >= MAX_NON_SUBSCRIBED["collection"]
        and not profile.is_subscriber
    )
    return 201, {"subscription_limit_reached": subscription_limit_reached, "ok": True}


@router.post("/survey/", response=Ok, url_name="plus.landing_page.survey", auth=None)
@ratelimit(group="landing_page_survey", key="user_or_ip", rate="100/m", block=True)
def post_survey(request, uuid: str = Form(...), response: Json = Form(...)):
    survey = get_object_or_404(LandingPageSurvey, uuid=uuid)
    survey.response = response
    survey.save()
    return True
