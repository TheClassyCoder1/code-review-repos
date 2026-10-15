from urllib.parse import urlparse

from flask import Flask, abort, g, request, session

from .config import INSECURE_DEV_KEY, Config, load_secret_key
from .db import close_db, get_db, init_db_command


def create_app(test_config=None):
    app = Flask(__name__)
    app.config.from_object(Config)
    app.config["SECRET_KEY"] = load_secret_key()
    if test_config:
        app.config.update(test_config)
    if app.config["SECRET_KEY"] == INSECURE_DEV_KEY and not app.config.get("TESTING"):
        raise RuntimeError("SECRET_KEY must be set in the environment")

    app.teardown_appcontext(close_db)
    app.cli.add_command(init_db_command)
    register_hooks(app)
    register_blueprints(app)
    return app


def _is_local_location(location):
    parsed = urlparse(location)
    if parsed.scheme or parsed.netloc:
        return parsed.scheme in ("http", "https") and parsed.netloc == request.host
    return location.startswith("/") and not location.startswith(("//", "/\\"))


def block_external_redirects(response):
    location = response.headers.get("Location")
    if location and 300 <= response.status_code < 400 and not _is_local_location(location):
        response.headers["Location"] = "/"
    return response


def register_hooks(app):
    @app.before_request
    def load_current_user():
        g.user = None
        user_id = session.get("user_id")
        if user_id is not None:
            g.user = get_db().execute(
                "SELECT id, email, role, guardian_id FROM users WHERE id = ?", (user_id,)
            ).fetchone()

    @app.before_request
    def enforce_admin_area():
        if request.path.startswith("/admin/"):
            if g.user is None or g.user["role"] != "admin":
                abort(403)


def register_blueprints(app):
    from .admin import admin_bp
    from .auth import auth_bp
    from .guardians.routes import guardians_bp
    from .invoices.routes import invoices_bp
    from .students.routes import students_bp
    from .transcripts.routes import transcripts_bp

    auth_bp.after_request(block_external_redirects)

    app.register_blueprint(auth_bp)
    app.register_blueprint(admin_bp, url_prefix="/admin")
    app.register_blueprint(students_bp, url_prefix="/students")
    app.register_blueprint(guardians_bp, url_prefix="/guardians")
    app.register_blueprint(invoices_bp, url_prefix="/invoices")
    app.register_blueprint(transcripts_bp, url_prefix="/transcripts")
