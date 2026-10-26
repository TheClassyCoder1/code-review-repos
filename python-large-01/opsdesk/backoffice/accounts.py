"""Back-office admin sign-in and OIDC sign-out."""

from django.conf import settings
from django.contrib import admin, auth
from django.contrib.auth.views import redirect_to_login
from django.core.exceptions import PermissionDenied
from django.http import HttpResponseRedirect
from django.shortcuts import redirect
from django.utils.http import url_has_allowed_host_and_scheme


def redirect_for_login(request):
    return redirect_to_login(request.get_full_path())


def login(request):
    # if the user has permission, just send them to the index page
    if request.method == 'GET' and admin.site.has_permission(request):
        next_path = request.GET.get(auth.REDIRECT_FIELD_NAME)
        return redirect(next_path or 'admin:index')
    # otherwise, they're logged in but don't have permission return a 403.
    elif request.user.is_authenticated:
        raise PermissionDenied
    else:
        return redirect_for_login(request)


def logout_url(request):
    """This gets called by mozilla_django_oidc when a user has signed out."""
    return (
        request.GET.get("next")
        or request.session.get("oidc_login_next")
        or getattr(settings, "LOGOUT_REDIRECT_URL", None)
        or "/"
    )


def oidc_logout(request):
    """Sign out, then follow ``logout_url`` only if it stays on this host."""
    target = logout_url(request)
    if not url_has_allowed_host_and_scheme(target, allowed_hosts={request.get_host()},
                                           require_https=request.is_secure()):
        target = settings.LOGOUT_REDIRECT_URL
    auth.logout(request)
    return HttpResponseRedirect(target)
