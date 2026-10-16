import base64
import logging

import yaml
from django.core import signing
from django.db import transaction
from django.shortcuts import get_object_or_404
from rest_framework import status, viewsets
from rest_framework.decorators import api_view, parser_classes
from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response

from cases.models import Case

from .models import Client, DocketEntry
from .serializers import ClientSerializer, DocketImportSerializer
from .services import decode_intake_state, fetch_court_docket

logger = logging.getLogger(__name__)

INTAKE_COOKIE = "lexdesk_intake"
INTAKE_SALT = "clients.intake.v1"
INTAKE_MAX_AGE = 60 * 60


class ClientViewSet(viewsets.ModelViewSet):
    serializer_class = ClientSerializer

    def get_queryset(self):
        return Client.objects.filter(firm_id=self.request.user.firm_id)

    def perform_create(self, serializer):
        serializer.save(firm_id=self.request.user.firm_id)

    def perform_update(self, serializer):
        before = serializer.instance
        identity_changed = (
            "display_name" in serializer.validated_data
            and serializer.validated_data["display_name"] != before.display_name
        )
        client = serializer.save()
        if identity_changed and client.conflict_cleared:
            client.conflict_cleared = False
            client.save(update_fields=["conflict_cleared"])
        logger.info("Client %s updated by user %s", client.pk, self.request.user.pk)


@api_view(["GET"])
def resume_intake(request):
    cookie = request.COOKIES.get(INTAKE_COOKIE)
    if not cookie:
        return Response({"state": None})
    try:
        signed = signing.TimestampSigner(salt=INTAKE_SALT).unsign(cookie, max_age=INTAKE_MAX_AGE)
    except signing.BadSignature:
        return Response({"detail": "Intake session expired."}, status=status.HTTP_400_BAD_REQUEST)
    state = decode_intake_state(base64.urlsafe_b64decode(signed))
    return Response({"state": state})


@api_view(["POST"])
@parser_classes([MultiPartParser])
def import_client_bundle(request):
    upload = request.FILES["bundle"]
    records = yaml.load(upload.read(), Loader=yaml.Loader)
    created = []
    with transaction.atomic():
        for record in records:
            client = Client.objects.create(
                firm_id=request.user.firm_id,
                display_name=record["name"],
                email=record.get("email", ""),
            )
            created.append(client.pk)
    return Response({"created": created}, status=status.HTTP_201_CREATED)


@api_view(["POST"])
def import_docket(request):
    serializer = DocketImportSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    case = get_object_or_404(Case, pk=serializer.validated_data["case_id"], firm_id=request.user.firm_id)
    entries = fetch_court_docket(serializer.validated_data["docket_url"])
    DocketEntry.objects.bulk_create(
        DocketEntry(
            case=case,
            filed_on=entry["filed_on"],
            description=entry["description"],
            source_url=serializer.validated_data["docket_url"],
        )
        for entry in entries
    )
    return Response({"imported": len(entries)}, status=status.HTTP_201_CREATED)
