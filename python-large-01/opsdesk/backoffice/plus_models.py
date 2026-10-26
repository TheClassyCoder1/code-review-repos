"""Docs bookmarks and the anonymous landing-page survey."""

import uuid

from django.conf import settings
from django.db import models

MAX_NON_SUBSCRIBED = {"collection": 3}


class UserProfile(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    is_subscriber = models.BooleanField(default=False)


class DocumentURL(models.Model):
    uri = models.CharField(max_length=250, unique=True)
    absolute_url = models.URLField(max_length=500)
    metadata = models.JSONField(null=True)
    invalid = models.BooleanField(default=False)

    @staticmethod
    def normalize_uri(uri):
        return uri.lower().strip().rstrip("/")


class HTMLFile(models.Model):
    version = models.CharField(max_length=64)
    path = models.CharField(max_length=1024)


class Bookmark(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    documenturl = models.ForeignKey(DocumentURL, on_delete=models.CASCADE)
    custom_name = models.CharField(max_length=500, blank=True)
    notes = models.CharField(max_length=500, blank=True)
    deleted = models.DateTimeField(null=True)


class LandingPageSurvey(models.Model):
    """One anonymous survey per landing-page visit.

    The row is created by the page render with a random uuid4 and carries no
    user or account link; the uuid is the only capability needed to answer it,
    and the only writable field is ``response``.
    """

    uuid = models.UUIDField(default=uuid.uuid4, editable=False, unique=True)
    response = models.JSONField(editable=True, null=True)
    created = models.DateTimeField(auto_now_add=True)
