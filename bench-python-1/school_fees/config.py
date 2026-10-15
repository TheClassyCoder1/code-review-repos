import os

import yaml

INSECURE_DEV_KEY = "dev-only-not-a-secret"


class Config:
    DATABASE = os.environ.get("SCHOOL_FEES_DB", "school_fees.sqlite3")
    UPLOAD_DIR = os.environ.get("GUARDIAN_UPLOAD_DIR", "/var/lib/school-fees/guardian-docs")
    TRANSCRIPT_DIR = os.environ.get("TRANSCRIPT_DIR", "/var/lib/school-fees/transcripts")
    EXPORT_DIR = os.environ.get("EXPORT_DIR", "/var/lib/school-fees/exports")
    REMINDER_WEBHOOK_URL = os.environ.get(
        "REMINDER_WEBHOOK_URL", "https://notify.internal.school.example/hooks/fee-reminders"
    )
    PAYMENT_GATEWAY_URL = os.environ.get("PAYMENT_GATEWAY_URL", "https://api.feegateway.example/v2")
    PAYMENT_GATEWAY_MERCHANT_KEY = os.environ.get("PAYMENT_GATEWAY_MERCHANT_KEY", "")
    YAML_LOADER = yaml.SafeLoader
    LATE_FEE_RATE = os.environ.get("LATE_FEE_RATE", "0.015")
    MAX_CONTENT_LENGTH = 5 * 1024 * 1024


def load_secret_key():
    key = os.environ.get("SECRET_KEY")
    if not key:
        return INSECURE_DEV_KEY
    return key
