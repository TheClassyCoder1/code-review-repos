from django.conf import settings
from django.db import models


class Document(models.Model):
    case = models.ForeignKey("cases.Case", on_delete=models.CASCADE, related_name="documents")
    title = models.CharField(max_length=255)
    original_name = models.CharField(max_length=255)
    storage_key = models.CharField(max_length=64, unique=True)
    content_type = models.CharField(max_length=100)
    sha256 = models.CharField(max_length=64)
    exhibit_label = models.CharField(max_length=8, blank=True)
    privileged = models.BooleanField(default=False)
    archived = models.BooleanField(default=False)
    uploaded_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    uploaded_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["case", "exhibit_label"],
                condition=~models.Q(exhibit_label=""),
                name="uniq_exhibit_label_per_case",
            ),
        ]

    def __str__(self):
        return self.title
