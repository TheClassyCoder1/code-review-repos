from django.utils import timezone
from rest_framework import serializers

from .models import Case


class CaseSerializer(serializers.ModelSerializer):
    client_name = serializers.CharField(source="client.display_name", read_only=True)

    class Meta:
        model = Case
        fields = [
            "id",
            "case_number",
            "title",
            "court_name",
            "status",
            "client",
            "client_name",
            "lead_attorney",
            "opened_on",
            "closed_on",
        ]
        read_only_fields = ["id", "client_name"]

    def validate_client(self, client):
        request = self.context["request"]
        if client.firm_id != request.user.firm_id:
            raise serializers.ValidationError("Unknown client.")
        return client

    def validate_opened_on(self, opened_on):
        if opened_on > timezone.localdate():
            raise serializers.ValidationError("A matter cannot be opened in the future.")
        return opened_on

    def validate(self, attrs):
        opened = attrs.get("opened_on", getattr(self.instance, "opened_on", None))
        closed = attrs.get("closed_on")
        if closed and opened and closed < opened:
            raise serializers.ValidationError({"closed_on": "Closed date precedes opened date."})
        return attrs
