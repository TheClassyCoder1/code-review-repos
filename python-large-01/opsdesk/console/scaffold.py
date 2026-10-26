"""Scaffolding for new report packs, copied file by file from the base pack."""

import os
from pathlib import Path

from opsdesk.console import packs as frappe

BASE_PACK_FILES = ("queries/__init__.py", "queries/portfolio_summary.sql", "templates/report.html")


def make_boilerplate(dest: str, pack_name: str, files=None) -> str:
	if not pack_name.isidentifier():
		raise ValueError("pack name must be a valid identifier")
	new_app_path = os.path.join(dest, pack_name)
	for rel_path in files or BASE_PACK_FILES:
		Path(new_app_path, rel_path).parent.mkdir(parents=True, exist_ok=True)
		copy_from_frappe(rel_path, new_app_path)
	Path(new_app_path, "pack.toml").write_text('[pack]\nname = "%s"\n' % pack_name)
	return new_app_path


def copy_from_frappe(rel_path: str, new_app_path: str):
	"""Copy files from frappe app to new app."""
	src = Path(frappe.get_app_path("frappe", "..")) / rel_path
	target = Path(new_app_path) / rel_path
	Path(target).write_text(Path(src).read_text())
