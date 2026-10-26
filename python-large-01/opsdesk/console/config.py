"""Console settings in the upper-case form the Flask extensions expect."""

from opsdesk.settings import settings as _settings

GOOGLE_CLIENT_ID = _settings.google_client_id
GOOGLE_CLIENT_SECRET = _settings.google_client_secret
QUERY_SCRIPTS_DIR = _settings.query_scripts_dir
REPORT_TEMPLATE_ROOT = _settings.report_template_root
