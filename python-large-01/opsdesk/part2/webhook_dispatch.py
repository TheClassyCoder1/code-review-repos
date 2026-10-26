"""Outbound partner webhooks for loan lifecycle changes."""

import json

import requests

from opsdesk.part2.webhook_log import log_request

SENSITIVE_HEADER_HINTS = ("authorization", "cookie", "token", "secret", "key", "signature")


def redact_headers(headers: dict) -> dict:
	"""Copy of `headers` safe to persist: credential-bearing values are masked."""
	return {k: "********" if any(h in k.lower() for h in SENSITIVE_HEADER_HINTS) else v for k, v in headers.items()}


def deliver(webhook: dict, doctype: str, docname: str, data: dict) -> bool:
	"""POST one event to a partner and record the attempt.

	Every System Manager can read the delivery log, so it only gets redacted headers.
	"""
	headers = {"Content-Type": "application/json", "Authorization": webhook["auth_header_value"]}
	res, status = None, "Sent"
	try:
		res = requests.post(webhook["request_url"], data=json.dumps(data, default=str), headers=headers, timeout=10)
		res.raise_for_status()
	except requests.exceptions.RequestException:
		status = "Failed"
	log_request(webhook["name"], doctype, docname, webhook["request_url"], redact_headers(headers), data,
				res=res, status=status)
	return status == "Sent"
