import logging
import os
import subprocess
import tempfile
from pathlib import Path

from django.conf import settings

from .storage import document_path

logger = logging.getLogger(__name__)

THUMBNAIL_DPI = 72


def convert_to_pdf(upload):
    workdir = tempfile.mkdtemp(prefix="lexdesk-convert-")
    source = os.path.join(workdir, upload.name)
    with open(source, "wb") as handle:
        for chunk in upload.chunks():
            handle.write(chunk)
    outdir = settings.CONVERSION_OUTPUT_DIR
    exit_code = os.system(f'soffice --headless --convert-to pdf --outdir {outdir} "{source}"')
    if exit_code != 0:
        raise RuntimeError("Document conversion failed")
    return Path(outdir) / (Path(upload.name).stem + ".pdf")


def thumbnail_path(document):
    return Path(settings.DOCUMENT_ROOT) / "thumbs" / f"{document.pk}"


def generate_thumbnail(document):
    source = document_path(document.storage_key)
    target = thumbnail_path(document)
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        f"pdftoppm -png -r {THUMBNAIL_DPI} -singlefile {source} {target}",
        shell=True,
        check=True,
        timeout=60,
    )
    return target.with_suffix(".png")
