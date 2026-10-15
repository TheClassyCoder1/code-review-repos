import re

from flask import Blueprint, abort, g, jsonify, request, send_file

from ..auth import login_required, role_required
from ..db import get_db
from ..students.repository import get_student, guardian_can_view
from . import render, storage

transcripts_bp = Blueprint("transcripts", __name__)

TRANSCRIPT_FILE_RE = re.compile(r"^S\d{7}-\d{4}[A-Z]{2}\.pdf$")


def _transcript_rows(student_id):
    return get_db().execute(
        "SELECT term_code, course, credits, grade_points FROM transcripts "
        "WHERE student_id = ? ORDER BY term_code, course",
        (student_id,),
    ).fetchall()


@transcripts_bp.route("/<int:student_id>/summary")
@login_required
def transcript_summary(student_id):
    if g.user["role"] == "guardian" and not guardian_can_view(g.user["guardian_id"], student_id):
        abort(404)
    rows = _transcript_rows(student_id)
    terms = sorted({row["term_code"] for row in rows})
    credits = sum(row["credits"] for row in rows)
    return jsonify(terms=terms, total_credits=credits, gpa=render.format_gpa(rows))


@transcripts_bp.route("/<int:student_id>/pdf", methods=["POST"])
@role_required("admin", "registrar")
def generate_pdf(student_id):
    student = get_student(student_id)
    if student is None:
        abort(404)
    rows = _transcript_rows(student_id)
    html = render.render_transcript_html(student, rows, render.format_gpa(rows))
    pdf_path = render.render_transcript_pdf(student_id, html)
    return send_file(pdf_path, mimetype="application/pdf")


@transcripts_bp.route("/files/<filename>")
@role_required("admin", "registrar")
def download_file(filename):
    if not TRANSCRIPT_FILE_RE.fullmatch(filename):
        abort(404)
    fh = storage.open_transcript_file(filename)
    if fh is None:
        abort(404)
    return send_file(fh, mimetype="application/pdf", download_name=filename)


@transcripts_bp.route("/exports", methods=["POST"])
@role_required("admin", "registrar")
def export_term():
    term_code = request.form.get("term", "").strip()
    if not term_code:
        return jsonify(error="term is required"), 400
    archive = render.export_transcript_archive(term_code)
    return jsonify(archive=archive), 202
