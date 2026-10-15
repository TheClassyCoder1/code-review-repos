from django.utils.html import escape
from django.utils.safestring import mark_safe

STATUS_COLOURS = {
    "intake": "grey",
    "open": "green",
    "stayed": "amber",
    "settled": "blue",
    "closed": "slate",
}


def case_status_badge(case):
    colour = STATUS_COLOURS.get(case.status, "grey")
    return mark_safe(
        f'<span class="badge badge-{colour}" data-status="{case.status}">{case.status.title()}</span>'
    )


def _year_suffix(opened_on):
    return opened_on.strftime("%y")


def format_case_number(firm_slug, sequence, opened_on):
    if sequence < 1:
        raise ValueError("sequence must be positive")
    prefix = firm_slug.upper()[:4]
    return f"{prefix}-{_year_suffix(opened_on)}-{sequence:05d}"


def team_summary(case):
    names = [escape(member.get_full_name()) for member in case.team.all()]
    return ", ".join(names)
