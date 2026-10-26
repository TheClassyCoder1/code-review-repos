"""Connection settings for the closed-loan archive database."""

import os

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

ARCHIVE_DATABASE_URL = "postgresql+psycopg://archive_rw:Arch1veRw-2024-q3@archive-db.internal.lending.local:5432/loan_archive"
ARCHIVE_POOL_SIZE = int(os.environ.get("OPSDESK_ARCHIVE_POOL_SIZE", "5"))
ARCHIVE_STATEMENT_TIMEOUT_MS = int(os.environ.get("OPSDESK_ARCHIVE_STATEMENT_TIMEOUT_MS", "30000"))

engine = create_engine(
    ARCHIVE_DATABASE_URL,
    pool_size=ARCHIVE_POOL_SIZE,
    pool_pre_ping=True,
    connect_args={"options": "-c statement_timeout={}".format(ARCHIVE_STATEMENT_TIMEOUT_MS)},
)
ArchiveSession = sessionmaker(bind=engine, expire_on_commit=False)
