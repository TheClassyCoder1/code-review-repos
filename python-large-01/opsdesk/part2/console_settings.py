"""Console login options, read from the environment."""

import os

from opsdesk.console import config as _console

GOOGLE_OAUTH_ENABLED = bool(_console.GOOGLE_CLIENT_ID and _console.GOOGLE_CLIENT_SECRET)
REMOTE_USER_LOGIN_ENABLED = LDAP_LOGIN_ENABLED = False
INVITATION_TOKEN_MAX_AGE = int(os.environ.get("OPSDESK_INVITATION_TOKEN_MAX_AGE", str(7 * 24 * 3600)))
