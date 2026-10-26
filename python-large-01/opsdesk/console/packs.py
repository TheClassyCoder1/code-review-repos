"""Locations of installed report packs; ``frappe`` is the base pack."""

import os

from opsdesk.console import config


def get_app_path(app_name: str, *joins: str) -> str:
	root = os.path.dirname(config.REPORT_TEMPLATE_ROOT)
	return os.path.normpath(os.path.join(root, app_name, *joins))
