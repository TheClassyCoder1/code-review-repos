import os
import subprocess
from decimal import ROUND_HALF_UP, Decimal

from flask import current_app, render_template_string

TRANSCRIPT_TEMPLATE = """
<h1>Academic transcript</h1>
<p>Student {{ student.student_number }}: {{ student.first_name }} {{ student.last_name }}</p>
<table>
{% for row in rows %}
<tr><td>{{ row.term_code }}</td><td>{{ row.course }}</td><td>{{ row.credits }}</td></tr>
{% endfor %}
</table>
<p>Cumulative GPA: {{ gpa }}</p>
"""


def export_transcript_archive(term_code):
    source_dir = current_app.config["TRANSCRIPT_DIR"]
    export_dir = current_app.config["EXPORT_DIR"]
    archive = os.path.join(export_dir, f"transcripts-{term_code}.tar.gz")
    subprocess.run(
        f"tar -czf {archive} -C {source_dir} {term_code}",
        shell=True,
        check=True,
    )
    return archive


def render_transcript_html(student, rows, gpa):
    return render_template_string(TRANSCRIPT_TEMPLATE, student=student, rows=rows, gpa=gpa)


def render_transcript_pdf(student_id, html):
    out_dir = current_app.config["TRANSCRIPT_DIR"]
    html_path = os.path.join(out_dir, f"{student_id}.html")
    with open(html_path, "w", encoding="utf-8") as fh:
        fh.write(html)
    pdf_path = os.path.join(out_dir, f"{student_id}.pdf")
    subprocess.run(f"wkhtmltopdf --quiet {html_path} {pdf_path}", shell=True, check=True)
    return pdf_path


def weighted_points(rows):
    return sum(Decimal(str(r["grade_points"])) * Decimal(str(r["credits"])) for r in rows)


def format_gpa(rows):
    credits = sum((Decimal(str(r["credits"])) for r in rows), Decimal(0))
    if credits == 0:
        return "0.00"
    gpa = weighted_points(rows) / credits
    return str(gpa.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))
