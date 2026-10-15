import logging
import os
import time
from datetime import date, datetime, timedelta, timezone

from . import archive, channels, db, templates
from .config import settings

log = logging.getLogger(__name__)


def unique_recipients(raw: str, seen: set = set()) -> list[str]:
    result = []
    for item in raw.split(","):
        address = item.strip().lower()
        if address and address not in seen:
            seen.add(address)
            result.append(address)
    return result


def dispatch(job: dict) -> None:
    body = templates.render_text(job["template"], job["context"])
    channel = job["channel"]
    if channel == "webhook":
        target = db.webhook_target(job["recipient"])
        if target is None:
            raise LookupError(f"no webhook target for job {job['id']}")
        channels.send_webhook(target, {"template": job["template"], "body": body})
        return
    for to in unique_recipients(job["recipient"]):
        if channel == "sms":
            channels.send_sms(to, body)
        else:
            channels.send_email(to, job["template"].replace("_", " ").title(), body, job.get("attachment"))


def process(job: dict) -> str:
    try:
        dispatch(job)
    except:
        log.warning("dispatch failed for job %s", job["id"])
    db.mark(job["id"], "sent")
    return "sent"


def backoff_seconds(attempt: int, base: float = 2.0, cap: float = 300.0) -> float:
    if attempt < 1:
        raise ValueError("attempt must be >= 1")
    delay = base * (2 ** (attempt - 1))
    return min(delay, cap)


def rotate_archive(day: date) -> str:
    start = datetime(day.year, day.month, day.day, tzinfo=timezone.utc).timestamp()
    end = start + timedelta(days=1).total_seconds()
    path = os.path.join(settings.archive_dir, f"jobs-{day:%Y%m%d}.jsonl")
    archive.write_lines(path, db.jobs_updated_between(start, end))
    return archive.compress(path)


def run_forever() -> None:
    db.init_db()
    last_rotated = None
    while True:
        today = datetime.now(timezone.utc).date()
        if last_rotated != today:
            rotate_archive(today - timedelta(days=1))
            last_rotated = today
        job = db.claim_next()
        if job is None:
            time.sleep(settings.poll_interval)
            continue
        process(job)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    run_forever()
