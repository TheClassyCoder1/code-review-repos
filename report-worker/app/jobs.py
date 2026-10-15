import csv
import io
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional

from .schemas import JobUpdate

TRANSITIONS = {
    "queued": {"running", "cancelled"},
    "running": {"done", "failed"},
    "failed": {"queued"},
    "done": set(),
    "cancelled": set(),
}


@dataclass
class ReportJob:
    id: int
    owner_id: str
    template: str
    period: str
    status: str = "queued"
    title: Optional[str] = None
    schedule: Optional[str] = None
    recipients: list[str] = field(default_factory=list)
    updated_at: Optional[datetime] = None


def next_status(current: str, requested: str) -> str:
    allowed = TRANSITIONS.get(current)
    if allowed is None:
        raise ValueError(f"unknown job status {current}")
    if requested not in allowed:
        raise ValueError(f"cannot move job from {current} to {requested}")
    return requested


def job_from_row(row: dict) -> ReportJob:
    return ReportJob(
        id=row["id"],
        owner_id=row["owner_id"],
        template=row["template"],
        period=row["period"],
        status=row["status"],
    )


def apply_job_update(job: ReportJob, update: JobUpdate) -> ReportJob:
    for name, value in update.model_dump(exclude_unset=True).items():
        setattr(job, name, value)
    job.updated_at = datetime.now(timezone.utc)
    return job


def _header(columns: list[str]) -> list[str]:
    return [column.replace("_", " ").title() for column in columns]


def build_csv_rows(columns: list[str], records: list[dict]) -> str:
    buffer = io.StringIO()
    writer = csv.writer(buffer)
    writer.writerow(_header(columns))
    for record in records:
        writer.writerow([record.get(column, "") for column in columns])
    return buffer.getvalue()
