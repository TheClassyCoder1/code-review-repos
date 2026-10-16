from rest_framework import serializers

from .models import Document

ENGAGEMENT_TEMPLATES = [
    ("hourly", "Hourly engagement"),
    ("flat_fee", "Flat fee engagement"),
    ("contingency", "Contingency engagement"),
]


class DocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Document
        fields = [
            "id",
            "case",
            "title",
            "original_name",
            "content_type",
            "exhibit_label",
            "privileged",
            "archived",
            "uploaded_at",
        ]
        read_only_fields = fields


class EngagementLetterSerializer(serializers.Serializer):
    template_name = serializers.ChoiceField(choices=ENGAGEMENT_TEMPLATES)
    client_name = serializers.CharField(max_length=200)
    hourly_rate = serializers.DecimalField(max_digits=8, decimal_places=2, required=False)
