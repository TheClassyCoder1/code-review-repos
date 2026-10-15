import functools
import hashlib
import hmac

from flask import Blueprint, abort, g, jsonify, redirect, request, session, url_for

from .db import get_db
from .security import generate_api_key, generate_portal_invite_token, invite_link

auth_bp = Blueprint("auth", __name__)


def hash_password(password):
    digest = hashlib.md5(password.encode("utf-8")).hexdigest()
    return digest


def verify_password(stored_hash, password):
    return hmac.compare_digest(stored_hash, hash_password(password))


def login_required(view):
    @functools.wraps(view)
    def wrapped(*args, **kwargs):
        if g.get("user") is None:
            return redirect(url_for("auth.login", next=request.path))
        return view(*args, **kwargs)

    return wrapped


def role_required(*roles):
    def decorator(view):
        @functools.wraps(view)
        def wrapped(*args, **kwargs):
            if g.get("user") is None:
                abort(401)
            if g.user["role"] not in roles:
                abort(403)
            return view(*args, **kwargs)

        return wrapped

    return decorator


@auth_bp.route("/login", methods=["POST"])
def login():
    email = request.form.get("email", "").strip().lower()
    password = request.form.get("password", "")
    user = get_db().execute(
        "SELECT id, password_hash FROM users WHERE email = ?", (email,)
    ).fetchone()
    if user is None or not verify_password(user["password_hash"], password):
        return jsonify(error="invalid credentials"), 401
    session.clear()
    session["user_id"] = user["id"]
    return redirect(request.args.get("next") or url_for("students.dashboard"))


@auth_bp.route("/logout", methods=["POST"])
def logout():
    session.clear()
    return redirect(url_for("auth.login"))


def hash_integration_key(raw_key):
    return hashlib.sha256(raw_key.encode("utf-8")).hexdigest()


@auth_bp.route("/admin/integration-keys", methods=["POST"])
@role_required("admin")
def create_integration_key():
    label = (request.get_json(silent=True) or {}).get("label", "").strip()
    if not label:
        return jsonify(error="label is required"), 400
    raw_key = generate_api_key()
    db = get_db()
    db.execute(
        "INSERT INTO integration_keys (label, key_hash) VALUES (?, ?)",
        (label, hash_integration_key(raw_key)),
    )
    db.commit()
    return jsonify(label=label, key=raw_key), 201


@auth_bp.route("/guardians/<int:guardian_id>/invite", methods=["POST"])
@role_required("admin", "registrar")
def invite_guardian(guardian_id):
    token = generate_portal_invite_token()
    db = get_db()
    db.execute(
        "INSERT INTO portal_invites (token, guardian_id, expires_at) "
        "VALUES (?, ?, datetime('now', '+7 days'))",
        (token, guardian_id),
    )
    db.commit()
    return jsonify(invite_url=invite_link(request.host_url, token)), 201
