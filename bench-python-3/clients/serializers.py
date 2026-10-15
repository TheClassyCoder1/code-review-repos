from rest_framework import serializers

from .models import Client
from .services import normalize_phone


class ClientSerializer(serializers.ModelSerializer):
    class Meta:
        model = Client
        fields = ["id", "display_name", "email", "phone", "is_organisation", "conflict_cleared"]
        read_only_fields = ["id", "conflict_cleared"]

    def validate_phone(self, value):
        if not value:
            return value
        try:
            return normalize_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError(str(exc))


class DocketImportSerializer(serializers.Serializer):
    case_id = serializers.IntegerField()
    docket_url = serializers.URLField()
