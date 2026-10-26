"""Masking helpers for anything that has to mention contact details in logs or tickets.

Repository rule: personal data never reaches a log line in clear text. Call sites that
need to correlate a log entry with a borrower should prefer internal ids; when an agent
needs a hint of which address was used, run it through these helpers first.
"""

import re

_EMAIL_RE = re.compile(r"^(?P<local>[^@\s]+)@(?P<domain>[^@\s]+)$")
_MASK = "***"


def mask_email(email: str | None) -> str:
    """``jane.doe@example.com`` -> ``j***@e***.com``. Unparseable input is fully masked."""
    if not email:
        return _MASK
    match = _EMAIL_RE.match(email.strip())
    if not match:
        return _MASK
    local = match.group("local")
    domain = match.group("domain")
    domain_name, _, tld = domain.rpartition(".")
    if not domain_name:
        domain_name, tld = domain, ""
    masked_domain = f"{domain_name[:1]}{_MASK}" + (f".{tld}" if tld else "")
    return f"{local[:1]}{_MASK}@{masked_domain}"


def mask_phone(phone: str | None) -> str:
    """Keep only the last two digits: ``+44 7700 900123`` -> ``***23``."""
    digits = re.sub(r"\D", "", phone or "")
    if len(digits) < 4:
        return _MASK
    return f"{_MASK}{digits[-2:]}"


def mask_national_id(national_id: str | None) -> str:
    """Keep only the last three characters of a national identifier."""
    value = (national_id or "").strip()
    if len(value) < 6:
        return _MASK
    return f"{_MASK}{value[-3:]}"
