"""Creates borrower support tickets in Zendesk on behalf of portal users.

Only internal ids are logged here: the requester's email and display name are
sent to Zendesk but never written to our logs.
"""

import logging
import os
from dataclasses import asdict, dataclass

import requests

logger = logging.getLogger(__name__)

ZENDESK_SUBDOMAIN = os.environ.get("OPSDESK_ZENDESK_SUBDOMAIN", "lending-support")
ZENDESK_API_USER = os.environ.get("OPSDESK_ZENDESK_API_USER", "")
ZENDESK_API_TOKEN = os.environ.get("OPSDESK_ZENDESK_API_TOKEN", "")
REQUEST_TIMEOUT = 15

PRODUCT_FIELD_ID = 360047198211


class ZendeskError(Exception):
    pass


@dataclass
class ZendeskUser:
    id: int | None
    verified: bool
    email: str
    name: str
    locale: str
    user_fields: dict | None = None
    external_id: str | None = None

    def as_payload(self) -> dict:
        return {k: v for k, v in asdict(self).items() if v is not None}


class ZendeskClient:
    """Thin wrapper around the Zendesk v2 REST API."""

    def __init__(self, subdomain=ZENDESK_SUBDOMAIN, api_user=ZENDESK_API_USER, api_token=ZENDESK_API_TOKEN):
        self.base_url = "https://{}.zendesk.com/api/v2".format(subdomain)
        self.session = requests.Session()
        self.session.auth = ("{}/token".format(api_user), api_token)

    def _get(self, path, **params):
        response = self.session.get(self.base_url + path, params=params, timeout=REQUEST_TIMEOUT)
        if response.status_code >= 400:
            raise ZendeskError("GET {} failed with {}".format(path, response.status_code))
        return response.json()

    def _post(self, path, payload):
        response = self.session.post(self.base_url + path, json=payload, timeout=REQUEST_TIMEOUT)
        if response.status_code >= 400:
            raise ZendeskError("POST {} failed with {}".format(path, response.status_code))
        return response.json()

    def get_user_by_email(self, email):
        users = self._get("/users/search.json", query="email:{}".format(email)).get("users") or []
        if not users:
            return None
        found = users[0]
        return ZendeskUser(
            id=found["id"],
            verified=found.get("verified", False),
            email=found["email"],
            name=found.get("name") or "",
            locale=found.get("locale") or "en-US",
            user_fields=found.get("user_fields"),
            external_id=found.get("external_id"),
        )

    def _user_to_zendesk_user(self, user, email):
        """Given a Django user, return a Zendesk user."""
        # Four possible cases to account for:
        # 1. No Django User, No ZD User -> Create ZD User From Email
        # 2. No Django User, ZD User -> Get Existing ZD User Using Email
        # 3. Django User, No ZD User -> Create ZD User from Django User
        # 4. Django User, ZD User -> Get ZD User, Update Django User with Zendesk ID
        if not user or not user.is_authenticated:
            # If the user already exists in Zendesk return
            # the Zendesk user object
            # instead of creating a new one
            if zuser := self.get_user_by_email(email):
                # No Django user, but yes ZD user
                return zuser
            # No Django user, no ZD user
            name = "Anonymous User"
            locale = "en-US"
            id = None
            external_id = None
            user_fields = None
        # Yes Django user
        else:
            fxa_uid = user.profile.fxa_uid
            id_str = user.profile.zendesk_id
            id = int(id_str) if id_str else None  # Yes Or No ZD User
            name = user.profile.display_name
            locale = user.profile.locale
            user_fields = {"user_id": fxa_uid}
            external_id = fxa_uid
        return ZendeskUser(
            id=id,
            verified=True,
            email=email or user.email,
            name=name,
            locale=locale,
            user_fields=user_fields,
            external_id=external_id,
        )

    def create_ticket(self, user, ticket_fields: dict) -> dict:
        """Open a ticket for ``user`` (a portal user or ``None`` for a guest).

        This is the only entry point that builds a requester. A guest has no
        account to fall back on, so a guest submission without an email is
        rejected before a requester is built for it.
        """
        email = (ticket_fields.get("email") or "").strip() or None
        authenticated = bool(user and user.is_authenticated)
        if not authenticated and email is None:
            raise ValueError("An email address is required to contact support without signing in.")

        requester = self._user_to_zendesk_user(user, email)
        ticket = {
            "subject": ticket_fields.get("subject") or "Support request",
            "comment": {"body": ticket_fields["description"]},
            "requester": requester.as_payload(),
            "tags": [ticket_fields.get("category", "general"), "portal"],
            "custom_fields": [{"id": PRODUCT_FIELD_ID, "value": ticket_fields.get("product")}],
        }
        created = self._post("/tickets.json", {"ticket": ticket})["ticket"]

        if authenticated and requester.id is None and created.get("requester_id"):
            user.profile.zendesk_id = str(created["requester_id"])
            user.profile.save(update_fields=["zendesk_id"])
        logger.info(
            "support ticket %s opened (portal user %s)", created.get("id"), user.pk if authenticated else "guest"
        )
        return created
