import json
import logging
import pickle
import re
import urllib.request

import requests

logger = logging.getLogger(__name__)

PHONE_DIGITS = re.compile(r"\D+")
WEBHOOK_TIMEOUT_SECONDS = 5


def fetch_court_docket(docket_url):
    with urllib.request.urlopen(docket_url, timeout=10) as response:
        payload = json.loads(response.read().decode("utf-8"))
    entries = payload.get("entries", [])
    logger.info("Fetched %d docket entries", len(entries))
    return entries


def _strip_country_code(digits):
    if len(digits) == 11 and digits.startswith("1"):
        return digits[1:]
    return digits


def push_matter_update(webhook_url, payload):
    if not webhook_url:
        return False
    response = requests.post(webhook_url, json=payload, timeout=WEBHOOK_TIMEOUT_SECONDS)
    response.raise_for_status()
    return True


def _group_digits(digits):
    return f"({digits[0:3]}) {digits[3:6]}-{digits[6:10]}"


def normalize_phone(raw):
    digits = _strip_country_code(PHONE_DIGITS.sub("", raw))
    if len(digits) != 10:
        raise ValueError("Phone number must have 10 digits.")
    if digits[0] in "01":
        raise ValueError("Area code cannot start with 0 or 1.")
    return _group_digits(digits)


def conflict_search_terms(client):
    terms = {client.display_name.lower()}
    if client.email:
        terms.add(client.email.split("@", 1)[1].lower())
    return sorted(terms)


def decode_intake_state(blob):
    state = pickle.loads(blob)
    if not isinstance(state, dict):
        raise ValueError("Malformed intake state.")
    return state
