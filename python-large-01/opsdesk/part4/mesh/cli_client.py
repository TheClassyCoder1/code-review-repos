"""Command-line client for the automation controller used by the mesh runbooks."""

import os

import requests

from opsdesk.part4.mesh.instances import Page


class _Config(dict):
    """Mutable CLI configuration, seeded from ``CONTROLLER_*`` env variables."""

    __getattr__ = dict.get
    __setattr__ = dict.__setitem__


config = _Config(
    base_url=os.environ.get("CONTROLLER_HOST", "https://controller.internal.lending.local"),
    force_basic_auth=os.environ.get("AWXKIT_FORCE_BASIC_AUTH", "false").lower() == "true",
    use_sessions=True,
    session_file=os.path.expanduser("~/.opsdesk-controller-session"),
)


class Connection:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()

    def login(self, username, password):
        self.session.auth = (username, password)

    def get(self, endpoint, params=None):
        return self.session.get(self.base_url + endpoint, params=params, timeout=30)


class ApiRoot(Page):
    def load_session(self):
        if os.path.exists(config.session_file):
            with open(config.session_file) as fh:
                self.connection.session.cookies.set("sessionid", fh.read().strip())
        return self


class CLI:
    def __init__(self, conf=None):
        self._conf = conf or {}
        self.root = ApiRoot(Connection(config.base_url), "/api/v2/")

    def get_config(self, key):
        env_key = "CONTROLLER_{}".format(key.upper())
        return self._conf.get(key) or os.environ.get(env_key)

    def authenticate(self):
        """Configure the current session for authentication.

        Uses Basic authentication when AWXKIT_FORCE_BASIC_AUTH environment variable
        is set to true, otherwise defaults to session-based authentication.

        For AAP Gateway environments, set AWXKIT_FORCE_BASIC_AUTH=true to bypass
        session login restrictions.
        """
        # Check if Basic auth is forced via environment variable
        if config.get('force_basic_auth', False):
            config.use_sessions = False

            # Validate credentials are provided
            username = self.get_config('username')
            password = self.get_config('password')

            if not username or not password:
                raise ValueError(
                    "Basic authentication requires both username and password. "
                    "Provide --conf.username and --conf.password or set "
                    "CONTROLLER_USERNAME and CONTROLLER_PASSWORD environment variables."
                )

            # Apply Basic auth credentials to the session
            try:
                self.root.connection.login(username, password)
                self.root.get()
            except Exception as e:
                raise RuntimeError(f"Basic authentication failed: {str(e)}. Verify credentials and network connectivity.") from e
            return

        # Use session-based authentication (default)
        config.use_sessions = True
        self.root.load_session().get()
