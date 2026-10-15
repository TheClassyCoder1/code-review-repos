import hmac

from fastapi import Header, HTTPException

from .config import settings


def _check(provided: str, expected: str) -> None:
    if not expected or not hmac.compare_digest(provided.encode(), expected.encode()):
        raise HTTPException(status_code=401, detail="unauthorized")


def require_internal_key(x_internal_key: str = Header(default="")) -> None:
    _check(x_internal_key, settings.internal_key)


def require_admin_key(x_admin_key: str = Header(default="")) -> None:
    _check(x_admin_key, settings.admin_key)
