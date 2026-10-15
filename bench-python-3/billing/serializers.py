from rest_framework import serializers

from .models import Invoice, TimeEntry


class TimeEntrySerializer(serializers.ModelSerializer):
    amount = serializers.DecimalField(max_digits=12, decimal_places=2, read_only=True)

    class Meta:
        model = TimeEntry
        fields = ["id", "case", "worked_on", "hours", "rate", "narrative", "billable", "amount"]
        read_only_fields = ["id", "rate", "amount"]

    def validate_hours(self, hours):
        if hours <= 0 or hours > 24:
            raise serializers.ValidationError("Hours must be between 0 and 24.")
        return hours


class InvoiceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Invoice
        fields = ["id", "case", "number", "issued_on", "due_on", "total", "paid"]
        read_only_fields = fields


class UtilizationQuerySerializer(serializers.Serializer):
    start = serializers.DateField()
    end = serializers.DateField()
    sort = serializers.CharField(required=False, default="billable")
    descending = serializers.BooleanField(required=False, default=True)
