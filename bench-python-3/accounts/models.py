from decimal import Decimal

from django.contrib.auth.models import AbstractUser
from django.db import models


class User(AbstractUser):
    ROLE_CHOICES = [("partner", "Partner"), ("associate", "Associate"), ("paralegal", "Paralegal")]

    firm = models.ForeignKey("cases.Firm", on_delete=models.PROTECT, related_name="members")
    role = models.CharField(max_length=16, choices=ROLE_CHOICES)
    hourly_rate = models.DecimalField(max_digits=8, decimal_places=2, default=Decimal("0.00"))


class ClientPortalAccount(models.Model):
    client = models.OneToOneField("clients.Client", on_delete=models.CASCADE, related_name="portal_account")
    email = models.EmailField(unique=True)
    password_hash = models.CharField(max_length=128)
    is_active = models.BooleanField(default=True)
    last_login = models.DateTimeField(null=True, blank=True)


class ApiKey(models.Model):
    firm = models.ForeignKey("cases.Firm", on_delete=models.CASCADE, related_name="api_keys")
    label = models.CharField(max_length=80)
    key_digest = models.CharField(max_length=64, unique=True)
    created_at = models.DateTimeField(auto_now_add=True)
    revoked = models.BooleanField(default=False)


class ShareLink(models.Model):
    document = models.ForeignKey("documents.Document", on_delete=models.CASCADE, related_name="share_links")
    token = models.CharField(max_length=64, unique=True)
    expires_at = models.DateTimeField()
    created_by = models.ForeignKey(User, on_delete=models.PROTECT)
