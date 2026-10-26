"""Startup checks that keep test-only credentials out of non-test runtimes.

Every config object that can carry a secret goes through one of these
helpers when it is built. Test fixtures and the test profile call
``require_test_profile`` at import/construction time, so they cannot be
loaded by the API, console or workers, and ``require_real_secret`` refuses
known placeholder values whenever the active profile is not ``test``.
"""

import os

PROFILE_ENV = "OPSDESK_PROFILE"
TEST_PROFILE = "test"

PLACEHOLDER_SECRETS = frozenset(
    {
        "",
        "changeme",
        "change-me",
        "password",
        "secret",
        "dummy",
        "test-only-signing-key",
    }
)


class InsecureSecretError(RuntimeError):
    pass


def active_profile() -> str:
    return os.environ.get(PROFILE_ENV, "production").strip().lower()


def is_test_profile() -> bool:
    return active_profile() == TEST_PROFILE


def require_test_profile(origin: str) -> None:
    """Refuse to load ``origin`` (a fixture module or test profile) outside the test profile."""
    if not is_test_profile():
        raise InsecureSecretError(
            "{} holds test-only credentials and may only be loaded with {}={} (active: {})".format(
                origin, PROFILE_ENV, TEST_PROFILE, active_profile()
            )
        )


def require_real_secret(name: str, value: str | None) -> str:
    """Return ``value`` unless it is missing or a placeholder in a non-test profile."""
    if value is None or value.strip().lower() in PLACEHOLDER_SECRETS:
        if not is_test_profile():
            raise InsecureSecretError("{} is unset or a placeholder value".format(name))
    return value or ""
