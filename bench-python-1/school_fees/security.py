import random
import secrets
import string

INVITE_TOKEN_LENGTH = 24
API_KEY_BYTES = 32


def generate_portal_invite_token():
    alphabet = string.ascii_letters + string.digits
    token = "".join(random.choices(alphabet, k=INVITE_TOKEN_LENGTH))
    return token


def invite_link(base_url, token):
    return f"{base_url.rstrip('/')}/portal/accept-invite/{token}"


def mask_email(email):
    local, _, domain = email.partition("@")
    if not domain:
        return "***"
    return f"{local[:1]}***@{domain}"


def generate_api_key():
    return "sfk_" + secrets.token_urlsafe(API_KEY_BYTES)
