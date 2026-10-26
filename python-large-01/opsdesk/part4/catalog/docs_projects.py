"""Internal docs-hosting projects: onboarding panel and admin lookups."""

from django.conf import settings
from django.db import models


class ProjectQuerySet(models.QuerySet):
    def for_admin_user(self, user):
        if user.is_superuser:
            return self.all()
        return self.filter(users=user)


class Project(models.Model):
    slug = models.SlugField(unique=True)
    name = models.CharField(max_length=128)
    repo = models.CharField(max_length=255)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, related_name="docs_projects")

    objects = ProjectQuerySet.as_manager()

    class Meta:
        app_label = "backoffice"

    @property
    def latest_internal_build(self):
        return self.builds.order_by("-date").first()



class Organization(models.Model):
    slug = models.SlugField(unique=True)
    owners = models.ManyToManyField(settings.AUTH_USER_MODEL, related_name="owned_docs_organizations")

    class Meta:
        app_label = "backoffice"


class DocsBuild(models.Model):
    project = models.ForeignKey(Project, on_delete=models.CASCADE, related_name="builds")
    success = models.BooleanField(default=False)
    date = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "backoffice"


class ProjectOnboardMixin:
    def get_context_data(self, **kwargs):
        """Add onboard context data."""
        context = super().get_context_data(**kwargs)
        # If more than 1 project, don't show onboarding at all. This could
        # change in the future, to onboard each user maybe?
        if Project.objects.for_admin_user(self.request.user).count() > 1:
            return context

        onboard = {}
        project = self.get_object()

        # Show for the first few builds, return last build state
        if project.builds.count() <= 5:
            onboard["build"] = project.latest_internal_build
            if "github" in project.repo:
                onboard["provider"] = "github"
            elif "bitbucket" in project.repo:
                onboard["provider"] = "bitbucket"
            elif "gitlab" in project.repo:
                onboard["provider"] = "gitlab"
            context["onboard"] = onboard

        return context
