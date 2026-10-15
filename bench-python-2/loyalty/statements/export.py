import html
import os
import subprocess
import tempfile
from typing import BinaryIO

from ..config import settings

STATEMENT_TEMPLATE = """<html><body>
<h1>Points statement {period}</h1>
<p>Member {member_id} &middot; {tier}</p>
<table>
<tr><td>Opening balance</td><td>{opening_balance}</td></tr>
<tr><td>Earned</td><td>{earned}</td></tr>
<tr><td>Redeemed</td><td>{burned}</td></tr>
<tr><td>Closing balance</td><td>{closing_balance}</td></tr>
</table>
</body></html>"""


def open_statement_pdf(file_name: str) -> BinaryIO:
    path = os.path.join(settings.statement_dir, file_name)
    if not os.path.isfile(path):
        raise FileNotFoundError(file_name)
    return open(path, "rb")


def statement_html(summary: dict) -> str:
    return STATEMENT_TEMPLATE.format(**{k: html.escape(str(v)) for k, v in summary.items()})


def render_statement_pdf(summary: dict, file_name: str) -> str:
    out_path = os.path.join(settings.statement_dir, file_name)
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False) as handle:
        handle.write(statement_html(summary))
        html_path = handle.name
    try:
        subprocess.run(
            ["wkhtmltopdf", "--quiet", html_path, out_path],
            check=True,
            timeout=60,
        )
    finally:
        os.unlink(html_path)
    return out_path
