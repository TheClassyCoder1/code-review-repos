"""Django settings for the back-office (all values from the environment)."""

import os

from opsdesk.settings import settings as _shared

SECRET_KEY, DEBUG, CI_ENV = _shared.secret_key, _shared.debug, _shared.ci_env
DEPLOY_ROOT, SITE_URL = _shared.deploy_root, _shared.site_url
ALLOWED_HOSTS = os.environ.get("OPSDESK_ALLOWED_HOSTS", "localhost").split(",")
ROOT_URLCONF = "opsdesk.backoffice.urls"
BOOKMARKS_BASE_URL = os.environ.get("OPSDESK_DOCS_BASE_URL", "https://docs.internal.lending.local")
LOGOUT_REDIRECT_URL = "/backoffice/"
INSTALLED_APPS = ["django.contrib.auth", "django.contrib.contenttypes", "django.contrib.sessions",
                  "django.contrib.messages", "helpdesk", "opsdesk.backoffice"]
