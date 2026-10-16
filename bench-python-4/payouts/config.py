import os
from decimal import Decimal

DATABASE_URL = os.environ.get("DATABASE_URL", "postgresql://localhost/payouts")
REDIS_URL = os.environ.get("REDIS_URL", "redis://localhost:6379/0")
TRIP_EVENTS_URL = os.environ.get("TRIP_EVENTS_URL", "redis://localhost:6379/1")

SECRET_KEY = os.environ["SECRET_KEY"]
OPS_API_KEY = os.environ["OPS_API_KEY"]
BANK_API_KEY = os.environ["BANK_API_KEY"]
BANK_WEBHOOK_SECRET = os.environ["BANK_WEBHOOK_SECRET"]

BANK_BASE_URL = os.environ.get("BANK_BASE_URL", "https://api.partnerbank.example/v2")
BANK_TIMEOUT_SECONDS = float(os.environ.get("BANK_TIMEOUT_SECONDS", "10"))

PUSH_BASE_URL = os.environ.get("PUSH_BASE_URL", "https://push.internal.example")
PUSH_TIMEOUT_SECONDS = float(os.environ.get("PUSH_TIMEOUT_SECONDS", "3"))

GEOCODER_URL = os.environ.get("GEOCODER_URL", "https://geo.internal.example/reverse")
GEOCODER_TIMEOUT_SECONDS = float(os.environ.get("GEOCODER_TIMEOUT_SECONDS", "2"))

# Flask rejects request bodies above this size with 413.
MAX_CONTENT_LENGTH = 5 * 1024 * 1024

DRIVER_TOKEN_MAX_AGE_SECONDS = 12 * 3600

COMMISSION_RATE = Decimal("0.20")
INSTANT_CASHOUT_FEE_RATE = Decimal("0.015")
MIN_INSTANT_CASHOUT = Decimal("10.00")

WEEKLY_INCENTIVE_TIERS = (
    (120, Decimal("150.00")),
    (80, Decimal("90.00")),
    (50, Decimal("40.00")),
)

EXPORT_DIR = os.environ.get("EXPORT_DIR", "/var/lib/payouts/exports")
