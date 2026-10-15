import os
import string
import subprocess
import uuid

from .config import settings

TEMPLATES = {
    "document_ready": "Your document for loan $loan_id is ready: $link",
    "repayment_due": "A repayment of $amount is due on $due_date.",
    "loan_approved": "Good news! Loan $loan_id was approved.",
}


def render_text(template: str, context: dict) -> str:
    source = TEMPLATES.get(template)
    if source is None:
        raise KeyError(f"unknown template {template}")
    body = string.Template(source).safe_substitute(context)
    if "$" in body:
        raise ValueError(f"template {template} is missing context values")
    return body


def render_pdf(job: dict, html: str) -> str:
    os.makedirs(settings.render_dir, exist_ok=True)
    src = os.path.join(settings.render_dir, f"{uuid.uuid4().hex}.html")
    dst = src[:-5] + ".pdf"
    with open(src, "w", encoding="utf-8") as fh:
        fh.write(html)
    title = job["context"].get("title", "Loan notice")
    subprocess.run(f"wkhtmltopdf --quiet --title '{title}' {src} {dst}", shell=True, check=True)
    os.remove(src)
    return dst
