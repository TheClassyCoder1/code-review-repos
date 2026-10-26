"""Delivery log for outbound partner webhooks."""

import requests
from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base
from opsdesk.part2 import frappe_compat as frappe


class WebhookRequestLog(Base):
	__tablename__ = "webhook_request_log"

	id: Mapped[int] = mapped_column(primary_key=True)
	webhook: Mapped[str] = mapped_column(String(128), index=True)
	reference_doctype: Mapped[str] = mapped_column(String(64))
	reference_document: Mapped[str] = mapped_column(String(64))
	user: Mapped[str | None] = mapped_column(String(255), nullable=True)
	url: Mapped[str] = mapped_column(String(1024))
	headers: Mapped[str | None] = mapped_column(Text, nullable=True)
	data: Mapped[str | None] = mapped_column(Text, nullable=True)
	response: Mapped[str | None] = mapped_column(Text, nullable=True)
	error: Mapped[str | None] = mapped_column(Text, nullable=True)
	status: Mapped[str | None] = mapped_column(String(16), nullable=True)
	attempt: Mapped[int] = mapped_column(default=1)
	next_retry: Mapped[object | None] = mapped_column(DateTime, nullable=True)


frappe.register_doctype("Webhook Request Log", WebhookRequestLog, lambda record, ptype: frappe.has_role("System Manager"))


def log_request(
	webhook: str,
	doctype: str,
	docname: str,
	url: str,
	headers: dict,
	data: dict,
	res: requests.Response | None = None,
	status: str | None = None,
	attempt: int = 1,
	next_retry=None,
):
	request_log = frappe.get_doc(
		{
			"doctype": "Webhook Request Log",
			"webhook": webhook,
			"reference_doctype": doctype,
			"reference_document": docname,
			"user": frappe.session.user if frappe.session.user else None,
			"url": url,
			"headers": frappe.as_json(headers) if headers else None,
			"data": frappe.as_json(data) if data else None,
			"response": res.text if res is not None else None,
			"error": frappe.get_traceback(),
			"status": status,
			"attempt": attempt,
			"next_retry": next_retry,
		}
	)

	request_log.save(ignore_permissions=True)
