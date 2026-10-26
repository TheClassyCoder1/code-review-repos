"""Environment-driven settings shared by the API, console and back-office."""

import os
from dataclasses import dataclass

_env = os.environ.get


@dataclass(frozen=True)
class Settings:
    database_url: str = _env("OPSDESK_DATABASE_URL", "postgresql+psycopg://opsdesk@localhost:5432/opsdesk")
    secret_key: str = _env("OPSDESK_SECRET_KEY", "")
    deploy_root: str = _env("OPSDESK_DEPLOY_ROOT", os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    site_url: str = _env("OPSDESK_SITE_URL", "http://localhost:8120")
    google_client_id: str = _env("OPSDESK_GOOGLE_CLIENT_ID", "")
    google_client_secret: str = _env("OPSDESK_GOOGLE_CLIENT_SECRET", "")
    query_scripts_dir: str = _env("OPSDESK_QUERY_SCRIPTS_DIR", "/srv/opsdesk/scripts")
    report_template_root: str = _env("OPSDESK_REPORT_TEMPLATE_ROOT", "/srv/opsdesk/report-packs/template")
    ci_orchestrator_url: str = _env("OPSDESK_CI_ORCHESTRATOR_URL", "")
    ci_build_id: str = _env("OPSDESK_CI_BUILD_ID", "")
    ci_instance_id: str = _env("OPSDESK_CI_INSTANCE_ID", "")
    debug: bool = _env("OPSDESK_DEBUG", "false").lower() == "true"
    ci_env: bool = _env("CI", "false").lower() == "true"


settings = Settings()
