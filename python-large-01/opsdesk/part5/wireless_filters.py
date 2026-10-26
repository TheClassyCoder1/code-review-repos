"""Filters for the branch-office wireless LAN inventory API."""

import django_filters
from django.db.models import Q

from .wireless_models import WirelessLAN, WirelessLANGroup


class WirelessLANFilterSet(django_filters.FilterSet):
    q = django_filters.CharFilter(method="search", label="Search")
    group_id = django_filters.ModelMultipleChoiceFilter(queryset=WirelessLANGroup.objects.all(), field_name="group")
    status = django_filters.MultipleChoiceFilter(choices=WirelessLAN.STATUS_CHOICES)
    auth_type = django_filters.MultipleChoiceFilter(choices=WirelessLAN.AUTH_TYPE_CHOICES)
    branch_code = django_filters.CharFilter(field_name="branch_code", lookup_expr="iexact")

    class Meta:
        model = WirelessLAN
        fields = ("id", "ssid", "description", "vlan_id")

    def search(self, queryset, name, value):
        if not value.strip():
            return queryset
        qs_filter = (
            Q(ssid__icontains=value) |
            Q(description__icontains=value)
        )
        return queryset.filter(qs_filter)
