import hashlib
import hmac
import json
import os
import smtplib
from email.message import EmailMessage
from typing import Optional

from .config import settings
from .http import session
from .schemas import WebhookTarget


def send_sms(to: str, body: str) -> str:
    resp = session.post(
        settings.sms_gateway_url,
        json={"to": to, "text": body},
        headers={"authorization": f"Bearer {settings.sms_api_key}"},
    )
    resp.raise_for_status()
    return resp.json()["message_id"]


def build_email(to: str, subject: str, body: str) -> EmailMessage:
    msg = EmailMessage()
    msg["From"] = settings.mail_from
    msg["To"] = to
    msg["Subject"] = subject
    msg.set_content(body)
    return msg


def load_attachment(name: str) -> bytes:
    path = os.path.join(settings.attachment_dir, name)
    with open(path, "rb") as fh:
        data = fh.read()
    if not data.startswith(b"%PDF"):
        raise ValueError(f"attachment {name} is not a pdf")
    return data


def send_email(to: str, subject: str, body: str, attachment: Optional[str] = None) -> None:
    msg = build_email(to, subject, body)
    if attachment:
        msg.add_attachment(load_attachment(attachment), maintype="application", subtype="pdf", filename=attachment)
    with smtplib.SMTP(settings.smtp_host, timeout=settings.http_timeout) as smtp:
        smtp.send_message(msg)


def send_webhook(target: WebhookTarget, payload: dict) -> int:
    body = json.dumps(payload, separators=(",", ":")).encode()
    signature = hmac.new(target.secret.encode(), body, hashlib.sha256).hexdigest()
    resp = session.post(
        target.url,
        data=body,
        headers={"content-type": "application/json", "x-notify-signature": signature},
    )
    resp.raise_for_status()
    return resp.status_code
