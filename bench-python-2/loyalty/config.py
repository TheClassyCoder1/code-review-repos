import os
from dataclasses import dataclass
from decimal import Decimal


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv(
        "LOYALTY_DATABASE_URL", "postgresql+asyncpg://loyalty@localhost/loyalty"
    )
    partner_api_base: str = os.getenv("PARTNER_API_BASE", "https://partners.example.net")
    partner_timeout_seconds: float = float(os.getenv("PARTNER_TIMEOUT_SECONDS", "8"))
    max_webhook_bytes: int = int(os.getenv("MAX_WEBHOOK_BYTES", "262144"))
    member_app_secret: str = os.getenv("MEMBER_APP_SECRET", "")
    admin_api_key: str = os.getenv("ADMIN_API_KEY", "")
    statement_dir: str = os.getenv("STATEMENT_DIR", "/var/lib/loyalty/statements")
    signup_bonus_points: int = 500
    referral_bonus_points: int = 1_000
    point_value: Decimal = Decimal("0.01")


settings = Settings()

TIER_NAMES = ("member", "silver", "gold", "platinum")
TIER_THRESHOLDS = (0, 5_000, 20_000, 50_000)
