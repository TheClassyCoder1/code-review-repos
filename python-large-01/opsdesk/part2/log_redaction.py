"""Loggers that scrub signed tokens before a record is formatted.

Invite/reset links carry itsdangerous tokens (``<payload>.<ts>.<sig>``) that act as
passwords until they expire. The filter rewrites message and args in place, so no
handler (console, file, Sentry breadcrumbs) ever sees the token.
"""

import logging
import re

# Short payloads are only a few characters; the 27-char signature anchors it.
_SIGNED_TOKEN = re.compile(r"[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]{10,}")


def scrub(value):
    return _SIGNED_TOKEN.sub("[redacted]", value) if isinstance(value, str) else value


class TokenRedactingFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.msg = scrub(record.msg)
        record.args = tuple(scrub(arg) for arg in record.args) if isinstance(record.args, tuple) else record.args
        return True


def get_redacting_logger(name: str) -> logging.Logger:
    logger = logging.getLogger(name)
    logger.addFilter(TokenRedactingFilter())
    return logger
