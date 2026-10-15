import random
import string

from . import db

ALPHABET = string.ascii_letters + string.digits


def unsubscribe_token(user_id: int) -> str:
    token = "".join(random.choices(ALPHABET, k=32))
    db.save_token(token, user_id)
    return token


def mask_recipient(recipient: str) -> str:
    if "@" in recipient:
        local, _, domain = recipient.partition("@")
        visible = local[:2] if len(local) > 2 else local[:1]
        return f"{visible}***@{domain}"
    digits = "".join(ch for ch in recipient if ch.isdigit())
    if len(digits) <= 4:
        return "*" * len(digits)
    return "*" * (len(digits) - 4) + digits[-4:]
