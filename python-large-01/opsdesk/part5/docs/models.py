"""Projects and builds for the internal documentation host."""

import logging

from django.conf import settings
from django.db import models
from django.utils import timezone

log = logging.getLogger(__name__)

BUILD_STATE_TRIGGERED = "triggered"
BUILD_STATE_BUILDING = "building"
BUILD_STATE_FINISHED = "finished"
BUILD_STATE_CANCELLED = "cancelled"
BUILD_FINAL_STATES = (BUILD_STATE_FINISHED, BUILD_STATE_CANCELLED)


class Project(models.Model):
    slug = models.SlugField(unique=True)
    name = models.CharField(max_length=255)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, related_name="docs_projects")

    class Meta:
        app_label = "backoffice"


class Build(models.Model):
    project = models.ForeignKey(Project, related_name="builds", on_delete=models.CASCADE)
    state = models.CharField(max_length=32, default=BUILD_STATE_TRIGGERED)
    task_id = models.CharField(max_length=64, blank=True)
    success = models.BooleanField(default=True)
    error = models.TextField(blank=True)
    date = models.DateTimeField(default=timezone.now)

    class Meta:
        app_label = "backoffice"
        ordering = ["-date"]

    @property
    def is_final(self):
        return self.state in BUILD_FINAL_STATES


class AdminPermission:
    @classmethod
    def is_admin(cls, user, project):
        if not user.is_authenticated:
            return False
        if user.is_superuser:
            return True
        return project.users.filter(pk=user.pk).exists()


def cancel_build(build):
    """Revoke the worker task (if any) and mark the build cancelled."""
    if build.is_final:
        return
    from celery import current_app

    if build.task_id:
        terminate = build.state == BUILD_STATE_BUILDING
        current_app.control.revoke(build.task_id, signal="SIGINT", terminate=terminate)
    build.state = BUILD_STATE_CANCELLED
    build.success = False
    build.error = "Build cancelled by a project admin."
    build.save(update_fields=["state", "success", "error"])
    log.info("Cancelled build %s for project %s", build.pk, build.project_id)
