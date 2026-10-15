import re

from flask import Blueprint, abort, g, jsonify, request
from markupsafe import Markup

from ..auth import login_required, role_required
from ..imports import parse_import_batch
from . import repository

students_bp = Blueprint("students", __name__)

STUDENT_NUMBER_RE = re.compile(r"^S\d{7}$")


@students_bp.route("/")
@login_required
def dashboard():
    if g.user["role"] == "guardian":
        rows = repository.students_for_guardian(g.user["guardian_id"])
        return jsonify(students=[dict(r) for r in rows])
    return jsonify(totals=repository.count_students_by_status())


@students_bp.route("/search")
@role_required("admin", "registrar", "bursar")
def search():
    name = request.args.get("q", "").strip()
    if len(name) < 2:
        return jsonify(error="query too short"), 400
    rows = repository.search_students(name, request.args.get("grade"))
    return jsonify(results=[dict(r) for r in rows])


def enrollment_badge(student):
    status = student["enrollment_status"]
    return Markup(f'<span class="badge badge-{status}">{status}</span>')


@students_bp.route("/<int:student_id>")
@login_required
def student_detail(student_id):
    student = repository.get_student(student_id)
    if student is None:
        abort(404)
    if g.user["role"] == "guardian" and not repository.guardian_can_view(
        g.user["guardian_id"], student_id
    ):
        abort(404)
    student["badge_html"] = str(enrollment_badge(student))
    return jsonify(student)


@students_bp.route("/", methods=["POST"])
@role_required("admin", "registrar")
def create():
    payload = request.get_json(silent=True) or {}
    number = str(payload.get("student_number", ""))
    if not STUDENT_NUMBER_RE.match(number):
        return jsonify(error="invalid student number"), 400
    try:
        grade = int(payload.get("grade_level"))
    except (TypeError, ValueError):
        return jsonify(error="grade_level must be an integer"), 400
    if not 1 <= grade <= 12:
        return jsonify(error="grade_level out of range"), 400
    student_id = repository.create_student(
        number, payload.get("first_name", ""), payload.get("last_name", ""), grade
    )
    return jsonify(id=student_id), 201


@students_bp.route("/import", methods=["POST"])
@role_required("admin", "registrar")
def bulk_import():
    batch = parse_import_batch(request.get_data())
    created = []
    for record in batch.get("students", []):
        created.append(
            repository.create_student(
                record["student_number"],
                record["first_name"],
                record["last_name"],
                int(record["grade_level"]),
            )
        )
    return jsonify(created=len(created)), 201
