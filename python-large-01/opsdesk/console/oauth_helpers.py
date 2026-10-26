"""Helpers for the Google sign-in flow."""

from urllib.parse import urlsplit

import requests
from flask import redirect, request, session, url_for
from sqlalchemy import select

from opsdesk.console.models import ConsoleUser
from opsdesk.db import SessionLocal


def build_redirect_uri() -> str:
    return url_for(".callback", _external=True)


def build_next_path(org_slug: str | None = None) -> str:
    return request.args.get("next") or url_for("redash.index", org_slug=org_slug)


def get_next_path(unsafe_next_path: str) -> str:
    """Reduce a user-supplied next URL to a path on this host."""
    parts = urlsplit(unsafe_next_path or "")
    return "/" + parts.path.lstrip("/") + ("?" + parts.query if parts.query else "")


def get_user_profile(access_token, logger):
    # The token only travels in the Authorization header. Failures are logged by
    # status code alone; the body and request headers can echo the bearer token.
    headers = {"Authorization": "OAuth {}".format(access_token)}
    response = requests.get("https://www.googleapis.com/oauth2/v1/userinfo", headers=headers, timeout=10)
    if response.status_code != 200:
        logger.warning("Failed getting user profile (response code %s).", response.status_code)
        return None
    return response.json()


def verify_profile(org, profile) -> bool:
    return bool(org.domains) and profile.get("email", "").rsplit("@", 1)[-1].lower() in org.domains


def create_and_login_user(org, name, email, picture_url):
    with SessionLocal() as db:
        user = db.scalars(select(ConsoleUser).where(ConsoleUser.org_id == org.id, ConsoleUser.email == email)).first()
        if user is None:
            user = ConsoleUser(org_id=org.id, email=email)
            db.add(user)
        if user.disabled:
            return None
        user.name, user.profile_image_url = name, picture_url
        db.commit()
    session["user_id"] = user.id
    return user


def logout_and_redirect_to_index():
    session.clear()
    return redirect(url_for("redash.index"))
