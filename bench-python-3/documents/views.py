import os

from django.conf import settings
from django.http import FileResponse
from django.shortcuts import get_object_or_404
from rest_framework import status, viewsets
from rest_framework.decorators import action, api_view, parser_classes
from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response

from cases.models import Case

from .conversion import convert_to_pdf, generate_thumbnail
from .models import Document
from .serializers import DocumentSerializer, EngagementLetterSerializer
from .storage import build_storage_key, checksum_upload, read_engagement_template, write_upload

CONVERTIBLE_TYPES = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"}


class DocumentViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = DocumentSerializer

    def get_queryset(self):
        queryset = Document.objects.filter(case__firm_id=self.request.user.firm_id, archived=False)
        case_id = self.request.query_params.get("case")
        if case_id is not None:
            queryset = queryset.filter(case_id=case_id)
        if self.request.user.role == "paralegal":
            queryset = queryset.filter(privileged=False)
        return queryset.order_by("-uploaded_at")

    @action(detail=True, methods=["get"])
    def content(self, request, pk=None):
        document = self.get_object()
        path = os.path.join(settings.DOCUMENT_ROOT, document.storage_key)
        return FileResponse(open(path, "rb"), as_attachment=True, filename=document.original_name)

    @action(detail=True, methods=["post"])
    def archive(self, request, pk=None):
        document = self.get_object()
        document.archived = True
        document.save(update_fields=["archived"])
        return Response(status=status.HTTP_204_NO_CONTENT)


@api_view(["POST"])
@parser_classes([MultiPartParser])
def upload_document(request):
    case = get_object_or_404(Case, pk=request.data.get("case"), firm_id=request.user.firm_id)
    upload = request.FILES["file"]
    if upload.content_type in CONVERTIBLE_TYPES:
        convert_to_pdf(upload)
    storage_key = build_storage_key(upload.content_type)
    document = Document.objects.create(
        case=case,
        title=request.data.get("title") or upload.name,
        original_name=upload.name,
        storage_key=storage_key,
        content_type=upload.content_type,
        sha256=checksum_upload(upload),
        uploaded_by=request.user,
    )
    write_upload(upload, storage_key)
    if upload.content_type == "application/pdf":
        generate_thumbnail(document)
    return Response(DocumentSerializer(document).data, status=status.HTTP_201_CREATED)


@api_view(["GET"])
def download_exhibit(request):
    relative = request.query_params.get("path", "")
    full_path = os.path.join(settings.EXHIBIT_ROOT, relative)
    if not os.path.isfile(full_path):
        return Response({"detail": "Exhibit not found."}, status=status.HTTP_404_NOT_FOUND)
    return FileResponse(open(full_path, "rb"), as_attachment=True)


@api_view(["POST"])
def render_engagement_letter(request):
    serializer = EngagementLetterSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    data = serializer.validated_data
    body = read_engagement_template(data["template_name"])
    letter = body.replace("{{ client_name }}", data["client_name"])
    if data.get("hourly_rate") is not None:
        letter = letter.replace("{{ hourly_rate }}", f"{data['hourly_rate']:.2f}")
    return Response({"letter": letter})
