from django.http import FileResponse
from django.shortcuts import get_object_or_404
from rest_framework import viewsets
from rest_framework.decorators import action, api_view
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from .models import Invoice, TimeEntry
from .reports import aging_buckets, utilization_report
from .serializers import InvoiceSerializer, TimeEntrySerializer, UtilizationQuerySerializer

REPORT_ORDERINGS = {
    "name": "u.last_name",
    "total": "total_hours",
    "billable": "billable_hours",
}


class TimeEntryViewSet(viewsets.ModelViewSet):
    serializer_class = TimeEntrySerializer

    def get_queryset(self):
        return TimeEntry.objects.filter(case__firm_id=self.request.user.firm_id).select_related("case")

    def perform_create(self, serializer):
        case = serializer.validated_data["case"]
        if case.firm_id != self.request.user.firm_id:
            raise ValidationError({"case": "Unknown matter."})
        if case.status == "closed":
            raise ValidationError({"case": "Time cannot be recorded against a closed matter."})
        serializer.save(attorney=self.request.user, rate=self.request.user.hourly_rate)


class InvoiceViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = InvoiceSerializer

    def get_queryset(self):
        return Invoice.objects.filter(case__firm_id=self.request.user.firm_id)

    @action(detail=False, methods=["get"])
    def aging(self, request):
        return Response(aging_buckets(self.get_queryset().filter(paid=False)))

    @action(detail=True, methods=["post"], url_path="mark-paid")
    def mark_paid(self, request, pk=None):
        invoice = self.get_object()
        invoice.paid = True
        invoice.save(update_fields=["paid"])
        return Response(self.get_serializer(invoice).data)


@api_view(["GET"])
def invoice_pdf(request, pk):
    invoice = get_object_or_404(Invoice, pk=pk)
    if not invoice.pdf_path:
        return Response({"detail": "Invoice PDF not generated yet."}, status=404)
    return FileResponse(open(invoice.pdf_path, "rb"), as_attachment=True, filename=f"{invoice.number}.pdf")


@api_view(["GET"])
def utilization(request):
    query = UtilizationQuerySerializer(data=request.query_params)
    query.is_valid(raise_exception=True)
    params = query.validated_data
    order_by = REPORT_ORDERINGS.get(params["sort"], REPORT_ORDERINGS["billable"])
    direction = "DESC" if params["descending"] else "ASC"
    rows = utilization_report(request.user.firm_id, params["start"], params["end"], order_by, direction)
    return Response({"rows": rows})
