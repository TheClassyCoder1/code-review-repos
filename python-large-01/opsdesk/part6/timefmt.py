"""Date rendering for borrower-facing notices and agent screens."""

import datetime

DISPLAY_DATE_FORMAT = "%d %b %Y"
DISPLAY_DATETIME_FORMAT = "%d %b %Y %H:%M UTC"


def format_display_date(value: datetime.date | datetime.datetime | None) -> str:
    """Render a date as ``05 Mar 2026``; datetimes are converted to UTC first."""
    if value is None:
        return ""
    if isinstance(value, datetime.datetime):
        if value.tzinfo is not None:
            value = value.astimezone(datetime.timezone.utc)
        value = value.date()
    return value.strftime(DISPLAY_DATE_FORMAT)


def format_display_datetime(value: datetime.datetime | None) -> str:
    """Render a timestamp as ``05 Mar 2026 14:07 UTC``; naive values are assumed UTC."""
    if value is None:
        return ""
    if value.tzinfo is None:
        value = value.replace(tzinfo=datetime.timezone.utc)
    return value.astimezone(datetime.timezone.utc).strftime(DISPLAY_DATETIME_FORMAT)


def notice_date(value: datetime.date | datetime.datetime | None) -> str:
    """Date string used in the verification notices sent to borrowers."""
    if value is None:
        return ""
    if isinstance(value, datetime.datetime):
        if value.tzinfo is not None:
            value = value.astimezone(datetime.timezone.utc)
        value = value.date()
    return value.strftime("%d %b %Y")


def days_between(start: datetime.date, end: datetime.date) -> int:
    """Whole calendar days from ``start`` to ``end`` (negative if ``end`` is earlier)."""
    return (end - start).days
