"""Projects, resources, source entities and their per-locale translations."""

from django.conf import settings
from django.db import models
from django.db.models import Q


class Locale(models.Model):
    code = models.CharField(max_length=20, unique=True)
    name = models.CharField(max_length=128)

    class Meta:
        app_label = "backoffice"


class ProjectQuerySet(models.QuerySet):
    def visible_for(self, user):
        visible = Q(disabled=False)
        return self.filter(visible if user.is_superuser else visible & Q(visibility=Project.Visibility.PUBLIC))


class Project(models.Model):
    class Visibility(models.TextChoices):
        PRIVATE = "private", "Private"
        PUBLIC = "public", "Public"

    slug = models.SlugField(unique=True)
    name = models.CharField(max_length=128)
    disabled = models.BooleanField(default=False)
    system_project = models.BooleanField(default=False)
    visibility = models.CharField(max_length=20, choices=Visibility.choices, default=Visibility.PRIVATE)

    objects = ProjectQuerySet.as_manager()

    class Meta:
        app_label = "backoffice"


class Tag(models.Model):
    slug = models.SlugField()
    name = models.CharField(max_length=64)

    class Meta:
        app_label = "backoffice"


class Resource(models.Model):
    class Format(models.TextChoices):
        FLUENT = "fluent", "fluent"
        PROPERTIES = "properties", "properties"
        DTD = "dtd", "dtd"

    project = models.ForeignKey(Project, on_delete=models.CASCADE, related_name="resources")
    path = models.TextField()
    format = models.CharField(max_length=20, choices=Format.choices)
    order = models.PositiveIntegerField(default=0)
    tag = models.ManyToManyField(Tag, blank=True, related_name="resources")

    class Meta:
        app_label = "backoffice"


class TranslatedResource(models.Model):
    resource = models.ForeignKey(Resource, on_delete=models.CASCADE, related_name="translatedresources")
    locale = models.ForeignKey(Locale, on_delete=models.CASCADE, related_name="translatedresources")

    class Meta:
        app_label = "backoffice"


class Entity(models.Model):
    resource = models.ForeignKey(Resource, on_delete=models.CASCADE, related_name="entities")
    string = models.TextField()
    key = models.JSONField(default=list)
    comment = models.TextField(blank=True)
    order = models.PositiveIntegerField(default=0)
    obsolete = models.BooleanField(default=False)
    date_created = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "backoffice"


class Translation(models.Model):
    entity = models.ForeignKey(Entity, on_delete=models.CASCADE)
    locale = models.ForeignKey(Locale, on_delete=models.CASCADE)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, on_delete=models.SET_NULL, related_name="+")
    string = models.TextField()
    date = models.DateTimeField(auto_now_add=True)
    approved = models.BooleanField(default=False)
    approved_user = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, on_delete=models.SET_NULL, related_name="+")
    approved_date = models.DateTimeField(null=True)
    rejected = models.BooleanField(default=False)
    active = models.BooleanField(default=False)

    class Meta:
        app_label = "backoffice"
