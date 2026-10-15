import os
from dataclasses import dataclass, field


def _csv(name: str, default: str) -> tuple:
    return tuple(h.strip() for h in os.getenv(name, default).split(",") if h.strip())


@dataclass(frozen=True)
class Settings:
    db_path: str = os.getenv("NOTIFY_DB_PATH", "/var/lib/notify/notify.db")
    internal_key: str = os.getenv("NOTIFY_WORKER_KEY", "")
    admin_key: str = os.getenv("NOTIFY_ADMIN_KEY", "")
    http_timeout: float = float(os.getenv("NOTIFY_HTTP_TIMEOUT", "5"))
    sms_gateway_url: str = os.getenv("SMS_GATEWAY_URL", "https://sms.example-gateway.com/v1/messages")
    sms_api_key: str = os.getenv("SMS_API_KEY", "")
    smtp_host: str = os.getenv("SMTP_HOST", "localhost")
    mail_from: str = os.getenv("NOTIFY_MAIL_FROM", "no-reply@portal.example.com")
    webhook_hosts: tuple = field(default_factory=lambda: _csv("NOTIFY_WEBHOOK_HOSTS", "hooks.partner-a.com,api.partner-b.io"))
    attachment_dir: str = os.getenv("NOTIFY_ATTACHMENT_DIR", "/var/lib/notify/attachments")
    archive_dir: str = os.getenv("NOTIFY_ARCHIVE_DIR", "/var/lib/notify/archive")
    render_dir: str = os.getenv("NOTIFY_RENDER_DIR", "/tmp/notify-render")
    poll_interval: float = float(os.getenv("NOTIFY_POLL_INTERVAL", "2"))
    max_attempts: int = int(os.getenv("NOTIFY_MAX_ATTEMPTS", "5"))


settings = Settings()
