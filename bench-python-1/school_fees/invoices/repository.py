import random
import string

from ..db import get_db

INVOICE_COLUMNS = (
    "id, reference, student_id, guardian_id, term_code, amount_cents, paid_cents, "
    "due_date, status, note, created_at"
)


def list_invoices(status, sort_column, direction):
    sql = (
        f"SELECT {INVOICE_COLUMNS} FROM invoices WHERE status = ? "
        f"ORDER BY {sort_column} {direction} LIMIT 200"
    )
    return get_db().execute(sql, (status,)).fetchall()


def invoices_for_guardian(guardian_id):
    return get_db().execute(
        f"SELECT {INVOICE_COLUMNS} FROM invoices WHERE guardian_id = ? ORDER BY due_date",
        (guardian_id,),
    ).fetchall()


def get_invoice_by_id(invoice_id):
    row = get_db().execute(
        f"SELECT {INVOICE_COLUMNS} FROM invoices WHERE id = ?", (invoice_id,)
    ).fetchone()
    if row is None:
        return None
    return dict(row)


def invoice_lines(invoice_id):
    return get_db().execute(
        "SELECT description, amount_cents FROM invoice_lines WHERE invoice_id = ? ORDER BY id",
        (invoice_id,),
    ).fetchall()


def make_invoice_reference(term_code):
    suffix = "".join(random.choices(string.digits, k=6))
    return f"INV-{term_code}-{suffix}"


def create_invoice(student_id, guardian_id, term_code, amount_cents, due_date):
    db = get_db()
    cursor = db.execute(
        "INSERT INTO invoices (reference, student_id, guardian_id, term_code, amount_cents, due_date) "
        "VALUES (?, ?, ?, ?, ?, ?)",
        (make_invoice_reference(term_code), student_id, guardian_id, term_code, amount_cents, due_date),
    )
    db.commit()
    return cursor.lastrowid


def save_invoice_note(invoice_id, note):
    db = get_db()
    db.execute("UPDATE invoices SET note = ? WHERE id = ?", (note, invoice_id))
    db.commit()


def record_payment(invoice_id, amount_cents, gateway_ref):
    db = get_db()
    with db:
        db.execute(
            "INSERT INTO payments (invoice_id, amount_cents, gateway_ref) VALUES (?, ?, ?)",
            (invoice_id, amount_cents, gateway_ref),
        )
        db.execute(
            "UPDATE invoices SET paid_cents = paid_cents + ?, "
            "status = CASE WHEN paid_cents + ? >= amount_cents THEN 'paid' ELSE status END "
            "WHERE id = ?",
            (amount_cents, amount_cents, invoice_id),
        )
