"""Desk modules a staff workspace can be assigned to."""

import frappe


@frappe.whitelist()
def get_assignable_modules():
	"""Modules a workspace can be assigned to, as `{module, label, app_name, app_title}`.

	Replaces `get_mountable_apps`: a workspace's rail placement follows its module now, so the
	question is which module owns it, not which app it was placed in.
	"""
	from frappe.utils.modules import is_module_visible

	modules = []
	for row in frappe.get_all("Module Def", fields=["name", "app_name"], order_by="app_name asc, name asc"):
		try:
			if not is_module_visible(row.name):
				continue
			# An unplaced module is in no app's dock and has no app to be titled after. Asking
			# `get_hooks` without an app name does not answer "no app": it returns the hook merged
			# across every installed one, so every module the site owns came back titled after
			# whichever app happened to be first.
			app_title = (
				(frappe.get_hooks("app_title", app_name=row.app_name) or [row.app_name])[0]
				if row.app_name
				else None
			)
			modules.append(
				{
					"module": row.name,
					"label": row.name,
					"app_name": row.app_name,
					"app_title": app_title,
				}
			)
		except Exception:
			# Nothing in this loop imports anything, so a failure here is a broken `Module Def`
			# row or a broken `app_title` hook, and no other code logs it. The module is skipped
			# rather than failing the whole picker, but it is skipped loudly: silently dropping
			# it left a module missing from New Workspace with no trace anywhere to explain why.
			frappe.log_error(f"Module {row.name} left out of the assignable list")
			continue
	return modules
