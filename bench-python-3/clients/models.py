from django.db import models


class Client(models.Model):
    firm = models.ForeignKey("cases.Firm", on_delete=models.CASCADE, related_name="clients")
    display_name = models.CharField(max_length=200)
    email = models.EmailField(blank=True)
    phone = models.CharField(max_length=20, blank=True)
    is_organisation = models.BooleanField(default=False)
    conflict_cleared = models.BooleanField(default=False)
    portal_invite_code = models.CharField(max_length=64, blank=True, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["display_name"]

    def __str__(self):
        return self.display_name


class DocketEntry(models.Model):
    case = models.ForeignKey("cases.Case", on_delete=models.CASCADE, related_name="docket_entries")
    filed_on = models.DateField()
    description = models.TextField()
    source_url = models.URLField(max_length=500)
