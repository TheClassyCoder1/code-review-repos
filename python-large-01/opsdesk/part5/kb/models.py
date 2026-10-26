"""Knowledge-base articles and their revisions."""

from django.db import models
from django.utils import timezone


class Topic(models.Model):
    slug = models.SlugField(unique=True)
    title = models.CharField(max_length=255)

    class Meta:
        app_label = "backoffice"


class Document(models.Model):
    title = models.CharField(max_length=255)
    slug = models.SlugField(max_length=255)
    locale = models.CharField(max_length=7, default="en-US", db_index=True)
    category = models.IntegerField(default=10)
    parent = models.ForeignKey("self", related_name="translations", null=True, blank=True, on_delete=models.CASCADE)
    current_revision = models.ForeignKey(
        "Revision", related_name="current_for", null=True, on_delete=models.SET_NULL
    )
    latest_localizable_revision = models.ForeignKey(
        "Revision", related_name="localizable_for", null=True, on_delete=models.SET_NULL
    )
    needs_change = models.BooleanField(default=False)
    needs_change_comment = models.CharField(max_length=500, blank=True)
    share_link = models.CharField(max_length=80, blank=True)
    products = models.ManyToManyField("backoffice.ProductClass", blank=True)
    topics = models.ManyToManyField(Topic, blank=True)

    class Meta:
        app_label = "backoffice"
        unique_together = [("parent", "locale"), ("slug", "locale")]


class Revision(models.Model):
    document = models.ForeignKey(Document, related_name="revisions", on_delete=models.CASCADE)
    content = models.TextField()
    is_approved = models.BooleanField(default=False)
    is_ready_for_localization = models.BooleanField(default=False)
    created = models.DateTimeField(default=timezone.now)
    creator_id = models.IntegerField()

    class Meta:
        app_label = "backoffice"
