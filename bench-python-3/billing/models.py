from decimal import Decimal

from django.conf import settings
from django.db import models


class TimeEntry(models.Model):
    case = models.ForeignKey("cases.Case", on_delete=models.CASCADE, related_name="time_entries")
    attorney = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    worked_on = models.DateField()
    hours = models.DecimalField(max_digits=5, decimal_places=2)
    rate = models.DecimalField(max_digits=8, decimal_places=2)
    narrative = models.TextField()
    billable = models.BooleanField(default=True)
    invoice = models.ForeignKey("Invoice", null=True, blank=True, on_delete=models.SET_NULL)

    @property
    def amount(self):
        return (self.hours * self.rate).quantize(Decimal("0.01"))


class Invoice(models.Model):
    case = models.ForeignKey("cases.Case", on_delete=models.PROTECT, related_name="invoices")
    number = models.CharField(max_length=32, unique=True)
    issued_on = models.DateField()
    due_on = models.DateField()
    total = models.DecimalField(max_digits=12, decimal_places=2)
    paid = models.BooleanField(default=False)
    pdf_path = models.CharField(max_length=255, blank=True)


class TrustBankConnection(models.Model):
    firm = models.OneToOneField("cases.Firm", on_delete=models.CASCADE)
    bank_name = models.CharField(max_length=120)
    host = models.CharField(max_length=255)
    client_id = models.CharField(max_length=120)
    api_secret = models.CharField(max_length=255)

    def __str__(self):
        return f"{self.bank_name} ({self.host})"


class TrustAccount(models.Model):
    firm = models.ForeignKey("cases.Firm", on_delete=models.CASCADE, related_name="trust_accounts")
    number = models.CharField(max_length=34)
    balance = models.DecimalField(max_digits=14, decimal_places=2, default=Decimal("0.00"))
    last_synced_at = models.DateTimeField(null=True, blank=True)
