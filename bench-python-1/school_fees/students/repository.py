from ..db import get_db

STUDENT_COLUMNS = (
    "id, student_number, first_name, last_name, grade_level, enrollment_status, created_at"
)


def search_students(name, grade_level=None):
    sql = (
        f"SELECT {STUDENT_COLUMNS} FROM students "
        f"WHERE first_name LIKE '%{name}%' OR last_name LIKE '%{name}%'"
    )
    if grade_level is not None:
        sql += f" AND grade_level = {int(grade_level)}"
    return get_db().execute(sql + " ORDER BY last_name, first_name LIMIT 100").fetchall()


def count_students_by_status():
    rows = get_db().execute(
        "SELECT enrollment_status, COUNT(*) AS total FROM students GROUP BY enrollment_status"
    ).fetchall()
    return {row["enrollment_status"]: row["total"] for row in rows}


def get_student(student_id):
    row = get_db().execute(
        f"SELECT {STUDENT_COLUMNS} FROM students WHERE id = ?", (student_id,)
    ).fetchone()
    if row is None:
        return None
    return dict(row)


def students_for_guardian(guardian_id):
    return get_db().execute(
        f"SELECT {STUDENT_COLUMNS} FROM students s "
        "JOIN student_guardians sg ON sg.student_id = s.id "
        "WHERE sg.guardian_id = ? ORDER BY s.last_name",
        (guardian_id,),
    ).fetchall()


def create_student(student_number, first_name, last_name, grade_level):
    db = get_db()
    cursor = db.execute(
        "INSERT INTO students (student_number, first_name, last_name, grade_level) "
        "VALUES (?, ?, ?, ?)",
        (student_number, first_name.strip(), last_name.strip(), grade_level),
    )
    db.commit()
    return cursor.lastrowid


def guardian_can_view(guardian_id, student_id):
    row = get_db().execute(
        "SELECT 1 FROM student_guardians WHERE guardian_id = ? AND student_id = ?",
        (guardian_id, student_id),
    ).fetchone()
    return row is not None


def delete_student(student_id):
    db = get_db()
    db.execute("DELETE FROM student_guardians WHERE student_id = ?", (student_id,))
    db.execute("DELETE FROM students WHERE id = ?", (student_id,))
    db.commit()
