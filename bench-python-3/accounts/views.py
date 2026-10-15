import logging

from django.contrib.auth import logout
from django.http import HttpResponseRedirect
from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from clients.models import Client

from .hashers import check_portal_password, hash_portal_password
from .models import ClientPortalAccount
from .serializers import PortalLoginSerializer, PortalSignupSerializer

logger = logging.getLogger(__name__)


@api_view(["POST"])
@permission_classes([AllowAny])
def portal_login(request):
    logger.info("Portal login attempt: %s", request.data)
    serializer = PortalLoginSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    account = ClientPortalAccount.objects.filter(
        email=serializer.validated_data["email"].lower(), is_active=True
    ).first()
    if account is None or not check_portal_password(serializer.validated_data["password"], account.password_hash):
        return Response({"detail": "Invalid credentials."}, status=status.HTTP_401_UNAUTHORIZED)
    request.session["portal_account_id"] = account.pk
    account.last_login = timezone.now()
    account.save(update_fields=["last_login"])
    return Response({"client": account.client_id})


def _invited_client(invite_code):
    return Client.objects.filter(portal_invite_code=invite_code, portal_account__isnull=True).first()


@api_view(["POST"])
@permission_classes([AllowAny])
def complete_signup(request):
    serializer = PortalSignupSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    data = serializer.validated_data
    client = _invited_client(data["invite_code"])
    if client is None:
        return Response({"detail": "Invite not found."}, status=status.HTTP_404_NOT_FOUND)
    account = ClientPortalAccount.objects.create(
        client=client, email=data["email"], password_hash=hash_portal_password(data["password"])
    )
    request.session["portal_account_id"] = account.pk
    return HttpResponseRedirect(data["return_path"])


def _clear_portal_session(request):
    request.session.pop("portal_account_id", None)
    request.session.modified = True


def logout_view(request):
    _clear_portal_session(request)
    logout(request)
    return HttpResponseRedirect(request.GET.get("next", "/"))
