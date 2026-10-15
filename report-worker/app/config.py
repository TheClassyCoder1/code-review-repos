import os
from dataclasses import dataclass

DEFAULT_RETURN_URL = "https://portal.example.com/billing"

RETURN_URLS = {
    "statements": "https://portal.example.com/billing/statements",
    "usage": "https://portal.example.com/billing/usage",
    "settings": "https://portal.example.com/billing/account",
}


@dataclass(frozen=True)
class Settings:
    db_path: str
    export_dir: str
    portal_api_key: str
    max_rows: int


def load_settings() -> Settings:
    max_rows = int(os.environ.get("REPORT_MAX_ROWS", "50000"))
    if max_rows <= 0:
        raise ValueError("REPORT_MAX_ROWS must be positive")
    return Settings(
        db_path=os.environ.get("REPORT_DB_PATH", "/var/lib/report-worker/reports.db"),
        export_dir=os.environ.get("REPORT_EXPORT_DIR", "/var/lib/report-worker/exports"),
        portal_api_key=os.environ["PORTAL_API_KEY"],
        max_rows=max_rows,
    )


def resolve_return_url(destination: str) -> str:
    return RETURN_URLS.get(destination, DEFAULT_RETURN_URL)
