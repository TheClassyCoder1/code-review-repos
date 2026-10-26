"""Back-office v3 API assembly (bearer-token auth, problem+json errors)."""

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured, PermissionDenied
from ninja import NinjaAPI
from ninja.security import HttpBearer

from opsdesk.part2.api_v3_errors import register_exception_handlers


class StaffBearer(HttpBearer):
    def authenticate(self, request, token: str):
        from rest_framework.authtoken.models import Token

        found = Token.objects.select_related("user").filter(key=token, user__is_active=True).first()
        return found.user if found else None


def require_perm(request, perm: str):
    # Fixed message: the 403 handler echoes str(exc) back to the caller.
    if not request.user.has_perm(perm):
        raise PermissionDenied("You do not have access to this resource.")


def build_api(*routers) -> NinjaAPI:
    # With DEBUG off the unhandled-exception handler re-raises (generic 500 page);
    # DEBUG is the only way exception text reaches a response, so refuse it off-host.
    if settings.DEBUG and not (settings.CI_ENV or set(settings.ALLOWED_HOSTS) <= {"localhost", "127.0.0.1"}):
        raise ImproperlyConfigured("OPSDESK_DEBUG must be false when the back-office is reachable off-host.")
    api = NinjaAPI(urls_namespace="v3", version="3", auth=StaffBearer())
    register_exception_handlers(api)
    for prefix, router in routers:
        api.add_router(prefix, router)
    return api
