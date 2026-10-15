import hashlib
import hmac
import re

MIN_PASSWORD_LENGTH = 12
PASSWORD_CLASSES = [re.compile(r"[a-z]"), re.compile(r"[A-Z]"), re.compile(r"\d"), re.compile(r"[^\w]")]


def hash_portal_password(raw_password):
    return hashlib.sha1(raw_password.encode("utf-8")).hexdigest()


def check_portal_password(raw_password, stored_hash):
    return hmac.compare_digest(hash_portal_password(raw_password), stored_hash)


def digest_api_key(raw_key):
    return hashlib.sha256(raw_key.encode("utf-8")).hexdigest()
