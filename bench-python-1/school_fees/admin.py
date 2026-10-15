from flask import Blueprint, abort, jsonify

from .db import get_db
from .students import repository as students

admin_bp = Blueprint("admin", __name__)


@admin_bp.route("/students/<int:student_id>", methods=["DELETE"])
def delete_student_record(student_id):
    if students.get_student(student_id) is None:
        abort(404)
    students.delete_student(student_id)
    return jsonify(status="deleted")


@admin_bp.route("/stats")
def stats():
    db = get_db()
    open_total = db.execute(
        "SELECT COALESCE(SUM(amount_cents - paid_cents), 0) FROM invoices WHERE status = 'open'"
    ).fetchone()[0]
    return jsonify(students=students.count_students_by_status(), outstanding_cents=open_total)
