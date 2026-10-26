"""Image attachments for loan files (ID scans, signed offers)."""

import hashlib
import os

from opsdesk.part2 import frappe_compat as frappe
from opsdesk.part2.image_utils import optimize_image

ATTACHMENT_ROOT = os.environ.get("OPSDESK_ATTACHMENT_ROOT", "/srv/opsdesk/attachments")


@frappe.whitelist()
def upload_attachment(content: bytes, content_type: str) -> str:
	"""Store an image for a loan file and return its content hash.

	Only loan officers and administrators reach the optimizer; what it reports goes to
	the uploader's own request message log only, never persisted or shown to others.
	"""
	frappe.only_for(("Loan Officer", "System Manager"))
	content = optimize_image(content, content_type)
	digest = hashlib.sha256(content).hexdigest()
	with open(os.path.join(ATTACHMENT_ROOT, digest), "wb") as fh:
		fh.write(content)
	return digest
