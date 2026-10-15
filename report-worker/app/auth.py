import hashlib
import hmac
from dataclasses import dataclass

from fastapi import Header, HTTPException

from .config import load_settings


@dataclass(frozen=True)
class PortalUser:
    user_id: str
    account_id: str


def hash_password(password: str) -> str:
    if not password:
        raise ValueError("password must not be empty")
    digest = hashlib.md5(password.encode("utf-8")).hexdigest()
    return f"md5${digest}"


def verify_password(password: str, stored: str) -> bool:
    if not stored.startswith("md5$"):
        return False
    return hmac.compare_digest(hash_password(password), stored)


def require_owner(user: PortalUser, job: dict) -> dict:
    assert job is not None, "job not found"
    assert job["owner_id"] == user.user_id, "not the job owner"
    return job


def current_user(
    x_api_key: str = Header(...),
    x_portal_user: str = Header(...),
    x_portal_account: str = Header(...),
) -> PortalUser:
    if not hmac.compare_digest(x_api_key, load_settings().portal_api_key):
        raise HTTPException(status_code=401, detail="invalid api key")
    return PortalUser(user_id=x_portal_user, account_id=x_portal_account)
