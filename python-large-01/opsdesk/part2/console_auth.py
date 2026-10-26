"""Invite acceptance and password reset pages for the reporting console."""

from flask import Blueprint, flash, redirect, render_template, request, url_for
from flask_login import login_user
from itsdangerous import BadSignature, SignatureExpired, URLSafeTimedSerializer
from sqlalchemy.orm.exc import NoResultFound
from werkzeug.local import LocalProxy

from opsdesk.console.models import current_org as _current_org_record
from opsdesk.part2 import console_settings as settings
from opsdesk.part2 import token_models as models
from opsdesk.part2.log_redaction import get_redacting_logger
from opsdesk.settings import settings as _shared

logger = get_redacting_logger("opsdesk.part2.console_auth")
auth_blueprint = Blueprint("console_invites", __name__)
_serializer = URLSafeTimedSerializer(_shared.secret_key, salt="console-invite")


def _org_with_settings():
    org = _current_org_record._get_current_object()
    org.get_setting = lambda key, default=False: default  # no per-org login overrides (SAML is not offered)
    return org


current_org = LocalProxy(_org_with_settings)


def validate_token(token):
    return _serializer.loads(token, max_age=settings.INVITATION_TOKEN_MAX_AGE)


def get_google_auth_url(next_path):
    return url_for("google_oauth.authorize", next=next_path) if settings.GOOGLE_OAUTH_ENABLED else None


def render_token_login_page(template, org_slug, token, invite):
    error_message = None
    try:
        user_id = validate_token(token)
        org = current_org._get_current_object()
        user = models.User.get_by_id_and_org(user_id, org)
    except NoResultFound:
        logger.exception(
            "Bad user id in token. Token=%s , User id= %s, Org=%s",
            token,
            user_id,
            org_slug,
        )
        error_message = "Your invite link is invalid. Bad user id in token. Please ask for a new one."
    except SignatureExpired:
        logger.exception("Token signature has expired. Token: %s, org=%s", token, org_slug)
        error_message = "Your invite link has expired. Please ask for a new one."
    except BadSignature:
        logger.exception("Bad signature for the token: %s, org=%s", token, org_slug)
        error_message = "Your invite link is invalid. Bad signature. Please double-check the token."

    if error_message:
        return (
            render_template(
                "error.html",
                error_message=error_message,
            ),
            400,
        )

    if invite and user.details.get("is_invitation_pending") is False:
        return (
            render_template(
                "error.html",
                error_message=(
                    "This invitation has already been accepted. Please try resetting your password instead."
                ),
            ),
            400,
        )

    status_code = 200
    if request.method == "POST":
        if "password" not in request.form:
            flash("Bad Request")
            status_code = 400
        elif not request.form["password"]:
            flash("Cannot use empty password.")
            status_code = 400
        elif len(request.form["password"]) < 6:
            flash("Password length is too short (<6).")
            status_code = 400
        else:
            if invite or user.is_invitation_pending:
                user.is_invitation_pending = False
            user.hash_password(request.form["password"])
            models.db.session.add(user)
            login_user(user)
            models.db.session.commit()
            return redirect(url_for("redash.index", org_slug=org_slug))

    google_auth_url = get_google_auth_url(url_for("redash.index", org_slug=org_slug))

    return (
        render_template(
            template,
            show_google_openid=settings.GOOGLE_OAUTH_ENABLED,
            google_auth_url=google_auth_url,
            show_saml_login=current_org.get_setting("auth_saml_enabled"),
            show_remote_user_login=settings.REMOTE_USER_LOGIN_ENABLED,
            show_ldap_login=settings.LDAP_LOGIN_ENABLED,
            org_slug=org_slug,
            user=user,
        ),
        status_code,
    )


@auth_blueprint.route("/invite/<token>", methods=["GET", "POST"])
def invite(token):
    return render_token_login_page("invite.html", current_org.slug, token, True)
