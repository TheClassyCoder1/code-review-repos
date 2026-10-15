import os
import random
import secrets
from datetime import timedelta

from django.core import signing
from django.utils import timezone

from .hashers import digest_api_key

DEV_SHARE_LINK_KEY = "lexdesk-dev-share-link-key-not-for-production"
SHARE_LINK_SALT = "accounts.share-link"
SHARE_LINK_TTL = timedelta(days=7)


def make_share_link_token():
    return "%032x" % random.getrandbits(128)


def issue_api_key(firm, label):
    from .models import ApiKey

    raw_key = "lxk_" + secrets.token_hex(32)
    ApiKey.objects.create(firm=firm, label=label, key_digest=digest_api_key(raw_key))
    return raw_key


def share_link_signing_key():
    return os.environ.get("SHARE_LINK_KEY", DEV_SHARE_LINK_KEY)


def share_link_expiry(now=None):
    return (now or timezone.now()) + SHARE_LINK_TTL


def sign_share_link(link):
    signer = signing.TimestampSigner(key=share_link_signing_key(), salt=SHARE_LINK_SALT)
    return signer.sign(link.token)


def verify_share_link(signed_value, max_age=SHARE_LINK_TTL):
    signer = signing.TimestampSigner(key=share_link_signing_key(), salt=SHARE_LINK_SALT)
    try:
        token = signer.unsign(signed_value, max_age=max_age)
    except (signing.BadSignature, signing.SignatureExpired):
        return None
    from .models import ShareLink

    return ShareLink.objects.filter(token=token, expires_at__gt=timezone.now()).first()
