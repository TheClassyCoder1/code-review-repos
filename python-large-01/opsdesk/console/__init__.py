"""Flask reporting console."""

from flask import Flask, redirect, url_for

from opsdesk.settings import settings


def create_console_app() -> Flask:
    from opsdesk.console.google_oauth import create_google_oauth_blueprint
    from opsdesk.console.reports import reports_blueprint

    app = Flask("opsdesk.console")
    app.config.update(SECRET_KEY=settings.secret_key, SESSION_COOKIE_SECURE=not settings.debug)
    app.register_blueprint(create_google_oauth_blueprint(app))
    app.register_blueprint(reports_blueprint, url_prefix="/reports")
    app.add_url_rule("/login", "redash.login", lambda org_slug=None: redirect(url_for("google_oauth.authorize")))
    app.add_url_rule("/", "redash.index", lambda org_slug=None: redirect(url_for("reports.list_reports")))
    return app
