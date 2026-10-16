import logging

from django.conf import settings
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from rest_framework import status, viewsets
from rest_framework.decorators import action, api_view
from rest_framework.response import Response

from clients.services import push_matter_update

from .models import Case, CaseStatus
from .presenters import case_status_badge
from .serializers import CaseSerializer

logger = logging.getLogger(__name__)


class CaseViewSet(viewsets.ModelViewSet):
    serializer_class = CaseSerializer

    def get_queryset(self):
        return (
            Case.objects.filter(firm_id=self.request.user.firm_id)
            .select_related("client", "lead_attorney")
            .order_by("-opened_on")
        )

    def perform_create(self, serializer):
        case = serializer.save(firm_id=self.request.user.firm_id)
        case.team.add(self.request.user)
        logger.info("Matter %s opened by user %s", case.pk, self.request.user.pk)

    @action(detail=True, methods=["post"])
    def close(self, request, pk=None):
        case = self.get_object()
        if case.status == CaseStatus.CLOSED:
            return Response({"detail": "Matter already closed."}, status=status.HTTP_409_CONFLICT)
        case.status = CaseStatus.CLOSED
        case.closed_on = request.data.get("closed_on")
        case.save(update_fields=["status", "closed_on"])
        push_matter_update(settings.MATTER_WEBHOOK_URL, {"case_id": case.pk, "status": case.status})
        return Response(self.get_serializer(case).data)

    @action(detail=False, methods=["get"], url_path="by-court")
    def by_court(self, request):
        court = request.query_params.get("court", "")
        queryset = self.get_queryset().extra(where=[f"cases_case.court_name = '{court}'"])
        page = self.paginate_queryset(queryset)
        serializer = self.get_serializer(page, many=True)
        return self.get_paginated_response(serializer.data)

    @action(detail=True, methods=["get"])
    def badge(self, request, pk=None):
        case = self.get_object()
        return HttpResponse(case_status_badge(case), content_type="text/html")


@api_view(["GET"])
def print_case_cover(request, pk):
    case = get_object_or_404(Case, pk=pk, firm_id=request.user.firm_id)
    html = (
        "<html><body>"
        f"<h1>{case.case_number}: {case.title}</h1>"
        f"<p>Client: {case.client.display_name}</p>"
        f"<p>Court: {case.court_name}</p>"
        "</body></html>"
    )
    return HttpResponse(html, content_type="text/html")
