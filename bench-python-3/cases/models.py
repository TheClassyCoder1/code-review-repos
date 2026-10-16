from django.conf import settings
from django.db import models
from django.db.models import Q


class Firm(models.Model):
    name = models.CharField(max_length=200)
    slug = models.SlugField(unique=True)

    def __str__(self):
        return self.name


class CaseStatus(models.TextChoices):
    INTAKE = "intake", "Intake"
    OPEN = "open", "Open"
    STAYED = "stayed", "Stayed"
    SETTLED = "settled", "Settled"
    CLOSED = "closed", "Closed"


class Case(models.Model):
    firm = models.ForeignKey(Firm, on_delete=models.CASCADE, related_name="cases")
    client = models.ForeignKey("clients.Client", on_delete=models.PROTECT, related_name="cases")
    case_number = models.CharField(max_length=40)
    title = models.CharField(max_length=255)
    court_name = models.CharField(max_length=120, blank=True)
    status = models.CharField(max_length=16, choices=CaseStatus.choices, default=CaseStatus.INTAKE)
    team = models.ManyToManyField(settings.AUTH_USER_MODEL, related_name="matters")
    lead_attorney = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="led_matters"
    )
    opened_on = models.DateField()
    closed_on = models.DateField(null=True, blank=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["firm", "case_number"], name="uniq_case_number_per_firm"),
            models.CheckConstraint(check=Q(status__in=CaseStatus.values), name="case_status_valid"),
        ]
        ordering = ["-opened_on"]

    def __str__(self):
        return f"{self.case_number} {self.title}"
