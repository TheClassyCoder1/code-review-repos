"""Certificate requests for internal service endpoints (console side)."""

import arrow
from flask import current_app

SECONDS_PER_YEAR = 365 * 24 * 60 * 60


# slug -> membership provider, filled in by the provider modules at import time
plugins: dict = {}


def is_valid_owner(email):
    user_membership_provider = None
    if current_app.config.get("USER_MEMBERSHIP_PROVIDER") is not None:
        user_membership_provider = plugins.get(current_app.config.get("USER_MEMBERSHIP_PROVIDER"))
    if user_membership_provider is None:
        # nothing to check since USER_MEMBERSHIP_PROVIDER is not configured
        return True

    # expecting owner to be an existing team DL
    return user_membership_provider.does_group_exist(email)


def get_duration(options):
    """
    Deduce certificate duration from options
    """
    validity_end = options.get("validity_end")
    if validity_end:
        return int((validity_end - arrow.utcnow()).total_seconds())
    else:
        return options.get("validity_years", 1) * SECONDS_PER_YEAR
