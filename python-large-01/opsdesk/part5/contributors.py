"""Activity summaries for translation contributors on the staff profile page."""

from django.contrib.humanize.templatetags.humanize import intcomma
from django.template.defaultfilters import pluralize

ACTION_TYPES = ("translation:created", "translation:approved", "translation:rejected")


def _action_label(action_type: str, count: int) -> str:
    match action_type:
        case "translation:created":
            return f"{intcomma(count)} translation{pluralize(count)}"
        case "translation:approved":
            return f"{intcomma(count)} approved"
        case "translation:rejected" | _:
            return f"{intcomma(count)} rejected"
