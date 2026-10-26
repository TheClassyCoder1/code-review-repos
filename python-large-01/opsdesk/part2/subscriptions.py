"""Back-office endpoint for storing the credit-bureau subscription credentials."""

import contextlib
import logging
import os

import requests
from django.conf import settings
from django.utils.encoding import smart_str
from django.utils.translation import gettext as _
from rest_framework import serializers, status
from rest_framework.generics import GenericAPIView
from rest_framework.permissions import IsAdminUser
from rest_framework.response import Response

from opsdesk.part2.licensing import TokenError, get_licenser

logger = logging.getLogger("opsdesk.part2.subscriptions")


def extend_schema_if_available(**kwargs):
    return lambda view: view  # drf-spectacular is not in the back-office image


@contextlib.contextmanager
def set_environ(**environ):
    """Temporarily apply `environ` (proxy settings for outbound calls)."""
    previous = dict(os.environ)
    os.environ.update({key: str(value) for key, value in environ.items()})
    try:
        yield
    finally:
        os.environ.clear()
        os.environ.update(previous)


class SubscriptionCredentialsSerializer(serializers.Serializer):
    subscriptions_username = serializers.CharField(required=False, allow_blank=True)
    subscriptions_password = serializers.CharField(required=False, allow_blank=True, write_only=True)
    subscriptions_client_id = serializers.CharField(required=False, allow_blank=True)
    subscriptions_client_secret = serializers.CharField(required=False, allow_blank=True, write_only=True)


class SubscriptionsView(GenericAPIView):
    permission_classes = (IsAdminUser,)
    serializer_class = SubscriptionCredentialsSerializer

    @extend_schema_if_available(
        extensions={'x-ai-description': 'List valid AAP subscriptions'},
    )
    def post(self, request):
        """Validate subscription credentials against the Red Hat portal and persist them on success."""
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        try:
            user = None
            pw = None
            basic_auth = False
            # determine if the credentials are for basic auth or not
            if data.get('subscriptions_client_id'):
                user, pw = data.get('subscriptions_client_id'), data.get('subscriptions_client_secret')
                if pw == '$encrypted$':
                    pw = settings.SUBSCRIPTIONS_CLIENT_SECRET
            elif data.get('subscriptions_username'):
                user, pw = data.get('subscriptions_username'), data.get('subscriptions_password')
                if pw == '$encrypted$':
                    pw = settings.SUBSCRIPTIONS_PASSWORD
                basic_auth = True

            if not user or not pw:
                return Response({"error": _("Missing subscription credentials")}, status=status.HTTP_400_BAD_REQUEST)

            with set_environ(**settings.AWX_TASK_ENV):
                validated = get_licenser().validate_rh(user, pw, basic_auth)

            # update settings if the credentials were valid
            if basic_auth:
                if user:
                    settings.SUBSCRIPTIONS_USERNAME = user
                if pw:
                    settings.SUBSCRIPTIONS_PASSWORD = pw
                # mutual exclusion for basic auth and service account
                # only one should be set at a given time so that
                # config/attach/ knows which credentials to use
                settings.SUBSCRIPTIONS_CLIENT_ID = ""
                settings.SUBSCRIPTIONS_CLIENT_SECRET = ""
            else:
                if user:
                    settings.SUBSCRIPTIONS_CLIENT_ID = user
                if pw:
                    settings.SUBSCRIPTIONS_CLIENT_SECRET = pw
                # mutual exclusion for basic auth and service account
                settings.SUBSCRIPTIONS_USERNAME = ""
                settings.SUBSCRIPTIONS_PASSWORD = ""
        except Exception as exc:
            msg = _("Invalid Subscription")
            if isinstance(exc, TokenError) or (
                isinstance(exc, requests.exceptions.HTTPError) and getattr(getattr(exc, 'response', None), 'status_code', None) == 401
            ):
                msg = _("The provided credentials are invalid (HTTP 401).")
            elif isinstance(exc, requests.exceptions.ProxyError):
                msg = _("Unable to connect to proxy server.")
            elif isinstance(exc, requests.exceptions.ConnectionError):
                msg = _("Could not connect to subscription service.")
            elif isinstance(exc, (ValueError, OSError)) and exc.args:
                msg = exc.args[0]
            else:
                logger.exception(smart_str("Invalid subscription submitted."), extra=dict(actor=request.user.username))
            return Response({"error": msg}, status=status.HTTP_400_BAD_REQUEST)

        return Response(validated)
