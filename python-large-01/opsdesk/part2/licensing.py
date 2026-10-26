"""Credit-bureau subscription checks (service account or portal login)."""

import os

import requests
from django.utils.translation import gettext as _

PORTAL_URL = os.environ.get("OPSDESK_BUREAU_PORTAL_URL", "https://entitlements.bureau.lending.local")
TIMEOUT = (3.05, 15)


class TokenError(Exception):
    """The service-account credentials were refused by the token endpoint."""


class SubscriptionLicenser:
    def _fetch(self, user: str, pw: str, basic_auth: bool) -> list:
        auth, headers = (user, pw), {}
        if not basic_auth:
            data = {"grant_type": "client_credentials", "client_id": user, "client_secret": pw}
            token = requests.post(PORTAL_URL + "/oauth/token", data=data, timeout=TIMEOUT)
            if token.status_code in (400, 401):
                raise TokenError("token request refused")
            token.raise_for_status()
            auth, headers = None, {"Authorization": "Bearer " + token.json()["access_token"]}
        resp = requests.get(PORTAL_URL + "/api/v1/subscriptions", auth=auth, headers=headers, timeout=TIMEOUT)
        resp.raise_for_status()
        return [s for s in resp.json()["results"] if s.get("status") == "active"]

    def validate_rh(self, user: str, pw: str, basic_auth: bool) -> list:
        """Return the active subscriptions visible to these credentials.

        Callers show this method's exception messages to the user. Only refused
        credentials and connection/proxy errors propagate unchanged; anything else
        becomes a fixed `ValueError`, so portal URLs and bodies never become the message.
        """
        try:
            active = self._fetch(user, pw, basic_auth)
        except (TokenError, requests.exceptions.ConnectionError):
            raise
        except requests.exceptions.HTTPError as exc:
            if getattr(exc.response, "status_code", None) == 401:
                raise
            raise ValueError(_("The subscription service returned an error. Try again later.")) from exc
        except Exception as exc:
            raise ValueError(_("Unexpected response from the subscription service.")) from exc
        if not active:
            raise ValueError(_("No active bureau subscriptions were found for these credentials."))
        return [{"subscription_id": s.get("id"), "product": s.get("product")} for s in active]


def get_licenser() -> SubscriptionLicenser:
    return SubscriptionLicenser()
