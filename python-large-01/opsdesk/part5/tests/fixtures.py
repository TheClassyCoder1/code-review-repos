"""Shared fixtures for the part of the suite that exercises archive and console auth."""

from opsdesk.part5 import secret_guard

secret_guard.require_test_profile(__name__)

ARCHIVE_TEST_USER = {
    "username": "archive-test",
    "password": "changeme",
    "database": "loan_archive_test",
}


def archive_test_url(host: str = "localhost", port: int = 5432) -> str:
    user = ARCHIVE_TEST_USER
    return "postgresql+psycopg://{}:{}@{}:{}/{}".format(
        user["username"], user["password"], host, port, user["database"]
    )
