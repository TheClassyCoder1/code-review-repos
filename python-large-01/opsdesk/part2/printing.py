"""Print-format builder preview for loan documents (offer letters, payoff quotes)."""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base
from opsdesk.part2 import frappe_compat as frappe
from opsdesk.part2.frappe_compat import _


class LoanOffer(Base):
	__tablename__ = "print_loan_offer"

	id: Mapped[int] = mapped_column(primary_key=True)
	loan_id: Mapped[str] = mapped_column(String(64), index=True)
	principal_minor: Mapped[int] = mapped_column(default=0)
	apr_bps: Mapped[int] = mapped_column(default=0)
	term_months: Mapped[int] = mapped_column(default=12)


frappe.register_doctype(
	"Loan Offer",
	LoanOffer,
	lambda record, ptype: frappe.has_role("System Manager") or (ptype == "print" and frappe.has_role("Loan Officer")),
)


@frappe.whitelist()
def render_jinja_template(template: str, doctype: str, docname: str) -> str:
	"""Render a raw Jinja2 template string with doc context (used by the print format builder preview)."""
	frappe.only_for("System Manager")
	doc = frappe.get_doc(doctype, docname)
	doc.check_permission("print")
	# template is rendered inside frappe's SandboxedEnvironment (Jinja2 sandbox).
	# The caller must hold the "print" permission on the document before reaching this line.
	try:
		return frappe.render_template(
			template, {"doc": doc}
		)  # nosemgrep: frappe-semgrep-rules.rules.security.frappe-ssti
	except Exception as e:
		# fail with 417 instead of 500 so the canvas can degrade inline
		# rather than the client popping the error-report dialog
		frappe.clear_last_message()
		frappe.throw(_("Failed to render template: {0}").format(str(e)), frappe.ValidationError)
