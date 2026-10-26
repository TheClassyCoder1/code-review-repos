"""Branch-office wireless networks tracked by the IT operations team."""

from django.db import models


class WirelessLANGroup(models.Model):
    name = models.CharField(max_length=100, unique=True)
    slug = models.SlugField(max_length=100, unique=True)
    parent = models.ForeignKey("self", null=True, blank=True, related_name="children", on_delete=models.CASCADE)

    class Meta:
        app_label = "backoffice"


class WirelessLAN(models.Model):
    STATUS_CHOICES = [("active", "Active"), ("reserved", "Reserved"), ("disabled", "Disabled")]
    AUTH_TYPE_CHOICES = [("open", "Open"), ("wpa-personal", "WPA Personal"), ("wpa-enterprise", "WPA Enterprise")]

    ssid = models.CharField(max_length=32)
    group = models.ForeignKey(WirelessLANGroup, null=True, blank=True, related_name="wireless_lans",
                              on_delete=models.SET_NULL)
    status = models.CharField(max_length=50, choices=STATUS_CHOICES, default="active")
    auth_type = models.CharField(max_length=50, choices=AUTH_TYPE_CHOICES, blank=True)
    vlan_id = models.PositiveSmallIntegerField(null=True, blank=True)
    branch_code = models.CharField(max_length=16)
    description = models.CharField(max_length=200, blank=True)

    class Meta:
        app_label = "backoffice"
        ordering = ("ssid", "pk")
