import sqlite3
from typing import Iterator, Optional

from .config import load_settings


def get_connection() -> Iterator[sqlite3.Connection]:
    conn = sqlite3.connect(load_settings().db_path)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()


def fetch_job(conn: sqlite3.Connection, job_id: int) -> Optional[dict]:
    row = conn.execute(
        "SELECT id, owner_id, title, schedule, status, template, period FROM report_jobs WHERE id = ?",
        (job_id,),
    ).fetchone()
    if row is None:
        return None
    return dict(row)


def insert_job(conn: sqlite3.Connection, owner_id: str, template: str, period: str) -> int:
    cur = conn.execute(
        "INSERT INTO report_jobs (owner_id, template, period, status) VALUES (?, ?, ?, 'queued')",
        (owner_id, template, period),
    )
    conn.commit()
    return int(cur.lastrowid)
