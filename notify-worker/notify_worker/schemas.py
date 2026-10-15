from typing import Literal, Optional
from urllib.parse import urlparse

from pydantic import BaseModel, Field, field_validator

from .config import settings

Channel = Literal["sms", "email", "webhook"]


class NotifyJob(BaseModel):
    user_id: int
    channel: Channel
    template: str = Field(pattern=r"^[a-z_]{3,40}$")
    to: str = Field(min_length=3, max_length=254)
    context: dict[str, str] = Field(default_factory=dict)
    attachment: Optional[str] = Field(default=None, pattern=r"^[A-Za-z0-9_-]{1,64}\.pdf$")


class WebhookTarget(BaseModel):
    partner_id: str = Field(min_length=1, max_length=64)
    url: str
    secret: str = Field(min_length=16)

    @field_validator("url")
    @classmethod
    def host_allowed(cls, value: str) -> str:
        parsed = urlparse(value)
        if parsed.scheme != "https" or parsed.hostname not in settings.webhook_hosts:
            raise ValueError("webhook host is not allowed")
        return value


class JobQuery(BaseModel):
    status: Optional[Literal["queued", "sent", "failed"]] = None
    sort: Literal["created_at", "updated_at", "attempts"] = "created_at"
    direction: Literal["asc", "desc"] = "desc"
    limit: int = Field(default=50, ge=1, le=500)
