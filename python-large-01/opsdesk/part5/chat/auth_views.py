"""Login page for the ops chat server (password, LDAP and two-factor flows)."""

from typing import Any

from django.conf import settings
from django.contrib.auth.views import LoginView as DjangoLoginView
from django.http import HttpRequest, HttpResponse, HttpResponseRedirect
from django.shortcuts import render
from django.template.response import SimpleTemplateResponse
from django.urls import reverse
from zerver.forms import OurAuthenticationForm
from zerver.lib.subdomains import get_subdomain, is_subdomain_root_or_alias
from zerver.lib.typed_endpoint import typed_endpoint
from zerver.lib.url_redirects import get_safe_redirect_to
from zerver.models import Realm
from zerver.models.realms import get_realm
from zerver.views.auth import (
    redirect_to_misconfigured_ldap_notice,
    social_auth_subdomain_login_page,
    start_two_factor_auth,
)
from zproject.backends import (
    ZulipLDAPConfigurationError,
    dev_auth_enabled,
    get_realm_from_request,
    is_2fa_verified,
    password_auth_enabled,
)


def redirect_to_deactivation_notice() -> HttpResponse:
    return HttpResponseRedirect(reverse("login_page_deactivated"))


def show_deactivation_notice(request: HttpRequest, next: str = "/") -> HttpResponse:
    realm = get_realm_from_request(request)
    context = {"deactivated_redirect": realm.deactivated_redirect if realm else None, "next": next}
    return render(request, "zerver/deactivated.html", context=context)


def login_context(request: HttpRequest) -> dict[str, Any]:
    realm = get_realm_from_request(request)
    return {
        "realm_name": realm.name if realm else None,
        "password_auth_enabled": password_auth_enabled(realm),
        "two_factor_authentication_enabled": settings.TWO_FACTOR_AUTHENTICATION_ENABLED,
    }


def update_login_page_context(request: HttpRequest, context: dict[str, Any]) -> None:
    for key in ("email", "already_registered"):
        if key in request.GET:
            context[key] = request.GET[key]
    if "is_deactivated" in request.GET:
        context["deactivated_account_error"] = True
        context["email"] = request.GET["is_deactivated"]


@typed_endpoint
def login_page(
    request: HttpRequest,
    /,
    *,
    next: str = "/",
    **kwargs: Any,
) -> HttpResponse:
    subdomain = get_subdomain(request)
    if subdomain == settings.SOCIAL_AUTH_SUBDOMAIN:
        return social_auth_subdomain_login_page(request)

    if subdomain == settings.SELF_HOSTING_MANAGEMENT_SUBDOMAIN:
        context = {
            "current_url": request.get_host(),
            "is_selfhosting_management_error_page": True,
        }
        return render(request, "zerver/invalid_realm.html", status=404, context=context)

    # To support previewing the Zulip login pages, we have a special option
    # that disables the default behavior of redirecting logged-in users to the
    # logged-in app.
    is_preview = "preview" in request.GET
    if settings.TWO_FACTOR_AUTHENTICATION_ENABLED:
        if request.user.is_authenticated and is_2fa_verified(request.user):
            redirect_to = get_safe_redirect_to(next, request.user.realm.url)
            return HttpResponseRedirect(redirect_to)
    elif request.user.is_authenticated and not is_preview:
        redirect_to = get_safe_redirect_to(next, request.user.realm.url)
        return HttpResponseRedirect(redirect_to)
    if is_subdomain_root_or_alias(request) and settings.ROOT_DOMAIN_LANDING_PAGE:
        return HttpResponseRedirect(reverse("realm_redirect", query=request.GET))

    realm = get_realm_from_request(request)
    if realm and realm.deactivated:
        if realm.deactivated_redirect:
            return show_deactivation_notice(request, next)
        return redirect_to_deactivation_notice()

    extra_context = kwargs.pop("extra_context", {})
    extra_context["next"] = next
    if dev_auth_enabled() and kwargs.get("template_name") == "zerver/development/dev_login.html":
        from zerver.views.development.dev_login import add_dev_login_context

        if "new_realm" in request.POST:
            try:
                realm = get_realm(request.POST["new_realm"])
            except Realm.DoesNotExist:
                realm = None

        add_dev_login_context(realm, extra_context)
        if realm and "new_realm" in request.POST:
            # If we're switching realms, redirect to that realm, but
            # only if it actually exists.
            return HttpResponseRedirect(realm.url)

    if "username" in request.POST:
        extra_context["email"] = request.POST["username"]
    extra_context.update(login_context(request))

    if settings.TWO_FACTOR_AUTHENTICATION_ENABLED:
        return start_two_factor_auth(request, extra_context=extra_context, **kwargs)

    try:
        template_response = DjangoLoginView.as_view(
            authentication_form=OurAuthenticationForm, extra_context=extra_context, **kwargs
        )(request)
    except ZulipLDAPConfigurationError as e:
        assert len(e.args) > 1
        return redirect_to_misconfigured_ldap_notice(request, e.args[1])

    if isinstance(template_response, SimpleTemplateResponse):
        # Only those responses that are rendered using a template have
        # context_data attribute. This attribute doesn't exist otherwise. It is
        # added in SimpleTemplateResponse class, which is a derived class of
        # HttpResponse. See django.template.response.SimpleTemplateResponse,
        # https://github.com/django/django/blob/2.0/django/template/response.py#L19
        assert template_response.context_data is not None
        update_login_page_context(request, template_response.context_data)

    assert isinstance(template_response, HttpResponse)
    return template_response
