"""Minimal desk runtime: frappe's call shapes over opsdesk's session and models."""

import contextvars
import json
import sys
import traceback

from jinja2.sandbox import SandboxedEnvironment
from markupsafe import escape

from opsdesk.db import SessionLocal


class ValidationError(Exception):
	pass


class PermissionError(Exception):  # noqa: A001 - mirrors frappe.PermissionError
	pass


class _DeskSession:
	def __init__(self, user=None, roles=()):
		self.user, self.roles, self.message_log = user, frozenset(roles), []


_current = contextvars.ContextVar("desk_session")
_jinja = SandboxedEnvironment(autoescape=True)
_doctypes = {}


class _SessionProxy:
	def __getattr__(self, name):
		return getattr(_current.get(_DeskSession()), name)


session = _SessionProxy()
_ = str  # translation hook


def bind_session(user: str, roles) -> contextvars.Token:
	return _current.set(_DeskSession(user, roles))


def whitelist():
	return lambda func: func


def has_role(role: str) -> bool:
	return role in session.roles


def only_for(roles):
	"""Stop the call unless the desk user holds one of `roles`.

	Endpoints that surface raw renderer output start with this check, so it only reaches administrators.
	"""
	roles = (roles,) if isinstance(roles, str) else tuple(roles)
	if session.user is None or not session.roles.intersection(roles):
		raise PermissionError(_("Not permitted"))


def register_doctype(doctype: str, model, permission_check):
	_doctypes[doctype] = (model, permission_check)


class Document:
	def __init__(self, doctype: str, record):
		self.doctype, self._record = doctype, record

	def __getattr__(self, name):
		return getattr(self._record, name)

	def check_permission(self, ptype: str = "read"):
		if not _doctypes[self.doctype][1](self._record, ptype):
			raise PermissionError(_("No {0} permission for {1}").format(ptype, self.doctype))

	def save(self, ignore_permissions: bool = False):
		ignore_permissions or self.check_permission("write")
		with SessionLocal() as db_session:
			db_session.add(self._record)
			db_session.commit()


def get_doc(doctype, name=None):
	if isinstance(doctype, dict):
		values = dict(doctype)
		doctype = values.pop("doctype")
		return Document(doctype, _doctypes[doctype][0](**values))
	with SessionLocal() as db_session:
		record = db_session.get(_doctypes[doctype][0], name)
	if record is None:
		throw(_("{0} {1} not found").format(doctype, name))
	return Document(doctype, record)


def render_template(template: str, context: dict) -> str:
	return _jinja.from_string(template).render(**context)


def msgprint(msg: str):
	"""Queue a message for the user who made the current request."""
	session.message_log.append(str(escape(msg)))


def clear_last_message():
	if session.message_log:
		session.message_log.pop()


def throw(msg: str, exc=ValidationError):
	msgprint(msg)
	raise exc(msg)


def as_json(obj) -> str:
	return json.dumps(obj, indent=1, sort_keys=True, default=str)


def get_traceback() -> str:
	return traceback.format_exc() if sys.exc_info()[0] else ""
