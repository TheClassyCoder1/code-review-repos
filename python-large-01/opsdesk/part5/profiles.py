"""Runtime profiles: which signing key and archive database a process uses."""

import os
from dataclasses import dataclass

from . import secret_guard


@dataclass(frozen=True)
class Profile:
    name: str
    signing_key: str
    archive_database_url: str


def _test_profile() -> Profile:
    secret_guard.require_test_profile("profiles.test")
    return Profile(
        name="test",
        signing_key=os.environ.get("OPSDESK_TEST_SIGNING_KEY", "test-only-signing-key"),
        archive_database_url=os.environ.get("OPSDESK_TEST_ARCHIVE_URL", "sqlite:///:memory:"),
    )


def _production_profile() -> Profile:
    signing_key = secret_guard.require_real_secret("OPSDESK_SIGNING_KEY", os.environ.get("OPSDESK_SIGNING_KEY"))
    archive_url = secret_guard.require_real_secret(
        "OPSDESK_ARCHIVE_DATABASE_URL", os.environ.get("OPSDESK_ARCHIVE_DATABASE_URL")
    )
    return Profile(name="production", signing_key=signing_key, archive_database_url=archive_url)


_PROFILES = {"test": _test_profile, "production": _production_profile}


def load_profile(name: str | None = None) -> Profile:
    name = (name or secret_guard.active_profile()).lower()
    try:
        factory = _PROFILES[name]
    except KeyError:
        raise ValueError("Unknown profile: {}".format(name)) from None
    return factory()
