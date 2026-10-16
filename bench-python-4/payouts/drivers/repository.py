from datetime import datetime, timezone

TRIP_COLUMNS = "id, external_id, fare, tip, booking_fee, distance_km, pickup_area, completed_at"


class DuplicateAccount(Exception):
    pass


def get_driver_name(conn, driver_id):
    row = conn.execute(
        "SELECT full_name FROM drivers WHERE id = %s",
        (driver_id,),
    ).fetchone()
    return row["full_name"]


def mask_iban(iban):
    if len(iban) <= 8:
        return "*" * len(iban)
    return f"{iban[:4]}{'*' * (len(iban) - 8)}{iban[-4:]}"


def get_payout_account(conn, driver_id):
    row = conn.execute(
        "SELECT iban, holder_name, verified_at FROM payout_accounts WHERE driver_id = %s",
        (driver_id,),
    ).fetchone()
    if row is None:
        return None
    return {
        "iban": mask_iban(row["iban"]),
        "holder_name": row["holder_name"],
        "verified": row["verified_at"] is not None,
    }


def normalise_iban(raw):
    return "".join(raw.split()).upper()


def register_payout_account(conn, driver_id, iban, holder_name):
    existing = conn.execute(
        "SELECT id FROM payout_accounts WHERE driver_id = %s",
        (driver_id,),
    ).fetchone()
    if existing is not None:
        raise DuplicateAccount(driver_id)
    row = conn.execute(
        "INSERT INTO payout_accounts (driver_id, iban, holder_name) "
        "VALUES (%s, %s, %s) RETURNING id",
        (driver_id, normalise_iban(iban), holder_name.strip()),
    ).fetchone()
    return row["id"]


def parse_cursor(value):
    if not value:
        return datetime.now(timezone.utc)
    return datetime.fromisoformat(value)


def list_driver_trips(conn, driver_id, before, limit):
    return conn.execute(
        f"SELECT {TRIP_COLUMNS} FROM trips "
        "WHERE driver_id = %s AND completed_at < %s "
        "ORDER BY completed_at DESC LIMIT %s",
        (driver_id, before, limit + 1),
    ).fetchall()
