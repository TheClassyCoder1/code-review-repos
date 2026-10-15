import json
import sqlite3
import time
from typing import Optional

from .config import settings
from .schemas import JobQuery, WebhookTarget

SCHEMA = """
CREATE TABLE IF NOT EXISTS jobs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    channel TEXT NOT NULL,
    template TEXT NOT NULL,
    recipient TEXT NOT NULL,
    context TEXT NOT NULL,
    attachment TEXT,
    status TEXT NOT NULL DEFAULT 'queued',
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at REAL NOT NULL,
    updated_at REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS webhook_targets (
    partner_id TEXT PRIMARY KEY,
    url TEXT NOT NULL,
    secret TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS unsubscribe_tokens (
    token TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    created_at REAL NOT NULL
);
"""


def connect() -> sqlite3.Connection:
    conn = sqlite3.connect(settings.db_path, isolation_level=None)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with connect() as conn:
        conn.executescript(SCHEMA)


def insert_job(job: dict) -> int:
    now = time.time()
    with connect() as conn:
        cur = conn.execute(
            "INSERT INTO jobs (user_id, channel, template, recipient, context, attachment, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (job["user_id"], job["channel"], job["template"], job["to"],
             json.dumps(job.get("context") or {}), job.get("attachment"), now, now),
        )
        return cur.lastrowid


def jobs_for_recipient(recipient: str) -> list[dict]:
    with connect() as conn:
        rows = conn.execute(
            "SELECT id, channel, template, status, created_at FROM jobs "
            f"WHERE recipient = '{recipient}' ORDER BY created_at DESC LIMIT 100"
        ).fetchall()
    return [dict(row) for row in rows]


def claim_next() -> Optional[dict]:
    with connect() as conn:
        conn.execute("BEGIN IMMEDIATE")
        row = conn.execute(
            "SELECT * FROM jobs WHERE status = 'queued' AND attempts < ? ORDER BY created_at LIMIT 1",
            (settings.max_attempts,),
        ).fetchone()
        if row is None:
            conn.execute("COMMIT")
            return None
        conn.execute(
            "UPDATE jobs SET status = 'sending', attempts = attempts + 1, updated_at = ? WHERE id = ?",
            (time.time(), row["id"]),
        )
        conn.execute("COMMIT")
    job = dict(row)
    job["context"] = json.loads(job["context"])
    return job


def list_jobs(query: JobQuery) -> list[dict]:
    sql = "SELECT id, user_id, channel, template, status, attempts, created_at, updated_at FROM jobs"
    params: list = []
    if query.status:
        sql += " WHERE status = ?"
        params.append(query.status)
    sql += f" ORDER BY {query.sort} {query.direction.upper()} LIMIT ?"
    params.append(query.limit)
    with connect() as conn:
        rows = conn.execute(sql, params).fetchall()
    return [dict(row) for row in rows]


def mark(job_id: int, status: str) -> None:
    with connect() as conn:
        conn.execute("UPDATE jobs SET status = ?, updated_at = ? WHERE id = ?", (status, time.time(), job_id))


def purge_before(cutoff: float) -> int:
    with connect() as conn:
        cur = conn.execute("DELETE FROM jobs WHERE status IN ('sent', 'failed') AND updated_at < ?", (cutoff,))
        return cur.rowcount


def jobs_updated_between(start: float, end: float) -> list[dict]:
    with connect() as conn:
        rows = conn.execute("SELECT * FROM jobs WHERE updated_at >= ? AND updated_at < ?", (start, end)).fetchall()
    return [dict(row) for row in rows]


def save_webhook_target(target: WebhookTarget) -> None:
    with connect() as conn:
        conn.execute(
            "INSERT OR REPLACE INTO webhook_targets (partner_id, url, secret) VALUES (?, ?, ?)",
            (target.partner_id, target.url, target.secret),
        )


def webhook_target(partner_id: str) -> Optional[WebhookTarget]:
    with connect() as conn:
        row = conn.execute("SELECT * FROM webhook_targets WHERE partner_id = ?", (partner_id,)).fetchone()
    return WebhookTarget(**dict(row)) if row else None


def save_token(token: str, user_id: int) -> None:
    with connect() as conn:
        conn.execute(
            "INSERT INTO unsubscribe_tokens (token, user_id, created_at) VALUES (?, ?, ?)",
            (token, user_id, time.time()),
        )


def user_for_token(token: str) -> Optional[int]:
    with connect() as conn:
        row = conn.execute("SELECT user_id FROM unsubscribe_tokens WHERE token = ?", (token,)).fetchone()
    return row["user_id"] if row else None
