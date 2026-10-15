import os
from pathlib import Path

from django.core.exceptions import ImproperlyConfigured

BASE_DIR = Path(__file__).resolve().parent.parent

DEBUG = os.environ.get("LEXDESK_DEBUG", "0") == "1"
SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "")
if not SECRET_KEY:
    raise ImproperlyConfigured("DJANGO_SECRET_KEY must be set")

if not DEBUG and not os.environ.get("SHARE_LINK_KEY"):
    raise ImproperlyConfigured("SHARE_LINK_KEY must be set outside DEBUG")

ALLOWED_HOSTS = os.environ.get("LEXDESK_ALLOWED_HOSTS", "localhost").split(",")

INSTALLED_APPS = [
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "rest_framework",
    "accounts",
    "cases",
    "clients",
    "documents",
    "billing",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
]

ROOT_URLCONF = "lexdesk.urls"
AUTH_USER_MODEL = "accounts.User"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("LEXDESK_DB_NAME", "lexdesk"),
        "USER": os.environ.get("LEXDESK_DB_USER", "lexdesk"),
        "PASSWORD": os.environ.get("LEXDESK_DB_PASSWORD", ""),
        "HOST": os.environ.get("LEXDESK_DB_HOST", "localhost"),
    }
}

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.SessionAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
        "cases.permissions.HasMatterAccess",
    ],
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 50,
}

DOCUMENT_ROOT = Path(os.environ.get("LEXDESK_DOCUMENT_ROOT", BASE_DIR / "var" / "documents"))
EXHIBIT_ROOT = DOCUMENT_ROOT / "exhibits"
ENGAGEMENT_TEMPLATE_DIR = BASE_DIR / "documents" / "engagement_templates"
CONVERSION_OUTPUT_DIR = DOCUMENT_ROOT / "converted"

MATTER_WEBHOOK_URL = os.environ.get("LEXDESK_MATTER_WEBHOOK_URL", "")
EFILING_BASE_URL = os.environ.get("LEXDESK_EFILING_URL", "https://efile.example-courts.gov/api/v2")

LOGGING = {
    "version": 1,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "root": {"handlers": ["console"], "level": "INFO"},
}
