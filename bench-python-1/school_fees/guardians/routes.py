import os

import requests
from flask import Blueprint, abort, current_app, g, jsonify, request, send_file

from ..auth import login_required, role_required
from ..db import get_db
from ..students.repository import guardian_can_view

guardians_bp = Blueprint("guardians", __name__)

PHOTO_MAX_BYTES = 2 * 1024 * 1024


def _guardian_or_404(guardian_id):
    row = get_db().execute(
        "SELECT id, full_name, email, phone FROM guardians WHERE id = ?", (guardian_id,)
    ).fetchone()
    if row is None:
        abort(404)
    return row


@guardians_bp.route("/<int:guardian_id>/documents/download")
@role_required("admin", "registrar")
def download_guardian_document(guardian_id):
    _guardian_or_404(guardian_id)
    filename = request.args.get("name", "")
    if not filename:
        abort(400)
    path = os.path.join(current_app.config["UPLOAD_DIR"], str(guardian_id), filename)
    return send_file(path, as_attachment=True)


@guardians_bp.route("/<int:guardian_id>/documents")
@role_required("admin", "registrar")
def list_documents(guardian_id):
    rows = get_db().execute(
        "SELECT id, filename, uploaded_at FROM guardian_documents WHERE guardian_id = ?",
        (guardian_id,),
    ).fetchall()
    return jsonify(documents=[dict(r) for r in rows])


@guardians_bp.route("/<int:guardian_id>/photo", methods=["POST"])
@role_required("admin", "registrar")
def import_guardian_photo(guardian_id):
    _guardian_or_404(guardian_id)
    payload = request.get_json(silent=True) or {}
    photo_url = payload.get("photo_url", "")
    response = requests.get(photo_url, timeout=5)
    response.raise_for_status()
    if len(response.content) > PHOTO_MAX_BYTES:
        return jsonify(error="photo too large"), 413
    db = get_db()
    db.execute("UPDATE guardians SET photo = ? WHERE id = ?", (response.content, guardian_id))
    db.commit()
    return jsonify(status="updated")


@guardians_bp.route("/<int:guardian_id>", methods=["PATCH"])
@role_required("admin", "registrar")
def update_contact(guardian_id):
    _guardian_or_404(guardian_id)
    payload = request.get_json(silent=True) or {}
    phone = str(payload.get("phone", "")).strip()[:32]
    db = get_db()
    db.execute("UPDATE guardians SET phone = ? WHERE id = ?", (phone, guardian_id))
    db.commit()
    return jsonify(status="updated")


@guardians_bp.route("/for-student/<int:student_id>")
@login_required
def list_guardians_for_student(student_id):
    if g.user["role"] == "guardian" and not guardian_can_view(g.user["guardian_id"], student_id):
        abort(404)
    rows = get_db().execute(
        "SELECT gd.id, gd.full_name, sg.relationship FROM guardians gd "
        "JOIN student_guardians sg ON sg.guardian_id = gd.id WHERE sg.student_id = ? "
        "ORDER BY gd.full_name",
        (student_id,),
    ).fetchall()
    return jsonify(guardians=[dict(r) for r in rows])
