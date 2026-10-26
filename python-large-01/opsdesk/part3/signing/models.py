from django.db import models


class Addon(models.Model):
    guid = models.CharField(max_length=255, unique=True, null=True)
    slug = models.SlugField(unique=True)
    name = models.CharField(max_length=255, default="")
    status = models.PositiveSmallIntegerField(default=0)

    def update_status(self):
        self.status = 3 if Version.unfiltered.filter(addon=self).exists() else 0
        self.save(update_fields=["status"])


class FileUpload(models.Model):
    uuid = models.UUIDField(unique=True)
    addon = models.ForeignKey(Addon, null=True, on_delete=models.CASCADE)
    version = models.CharField(max_length=255)
    created = models.DateTimeField(auto_now_add=True)


class Version(models.Model):
    addon = models.ForeignKey(Addon, on_delete=models.CASCADE)
    version = models.CharField(max_length=255)
    channel = models.PositiveSmallIntegerField()

    unfiltered = models.Manager()

    class Meta:
        # Version numbers are never reused: when two submissions of one version
        # both pass the "already exists?" checks, the loser's INSERT fails here
        # and its atomic block rolls back, so no duplicate is ever committed.
        constraints = [models.UniqueConstraint(fields=("addon", "version"), name="version_addon_version_uniq")]

    @classmethod
    def from_upload(cls, upload, addon, channel, *, parsed_data, **kwargs):
        return cls.unfiltered.create(addon=addon, version=parsed_data["version"], channel=channel)
