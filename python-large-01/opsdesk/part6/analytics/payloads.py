"""Event payloads for the external funnel-analytics vendor.

The vendor is a third party outside our data-processing agreement for customer data, so
anything that goes through ``build_event`` is restricted to internal identifiers and
coarse enums. Any property not on the allowlist raises instead of being sent.
"""

import datetime

ALLOWED_PROPERTIES = frozenset(
    {
        "borrower_id",
        "identity_id",
        "loan_id",
        "status",
        "outcome",
        "country",
        "step",
        "duration_seconds",
    }
)


class DisallowedPropertyError(ValueError):
    pass


def build_event(event: str, **properties) -> dict:
    """Return a vendor event body containing only allowlisted, scalar properties."""
    disallowed = sorted(set(properties) - ALLOWED_PROPERTIES)
    if disallowed:
        raise DisallowedPropertyError(f"properties not allowed in analytics events: {disallowed}")
    for key, value in properties.items():
        if value is not None and not isinstance(value, (int, float, str, bool)):
            raise DisallowedPropertyError(f"property {key} must be a scalar")
    return {
        "event": event,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "properties": {key: value for key, value in properties.items() if value is not None},
    }
