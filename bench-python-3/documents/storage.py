import hashlib
import random
import string
import uuid
from pathlib import Path

from django.conf import settings

ALLOWED_EXTENSIONS = {
    "application/pdf": ".pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ".docx",
    "image/png": ".png",
    "image/jpeg": ".jpg",
}
CHUNK_SIZE = 64 * 1024


def build_storage_key(content_type):
    extension = ALLOWED_EXTENSIONS.get(content_type)
    if extension is None:
        raise ValueError(f"Unsupported content type: {content_type}")
    return f"{uuid.uuid4().hex}{extension}"


def read_engagement_template(template_name):
    path = Path(settings.ENGAGEMENT_TEMPLATE_DIR) / f"{template_name}.txt"
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def document_path(storage_key):
    return Path(settings.DOCUMENT_ROOT) / storage_key


def checksum_upload(upload):
    digest = hashlib.sha256()
    for chunk in upload.chunks(CHUNK_SIZE):
        digest.update(chunk)
    upload.seek(0)
    return digest.hexdigest()


def write_upload(upload, storage_key):
    target = document_path(storage_key)
    target.parent.mkdir(parents=True, exist_ok=True)
    with open(target, "wb") as handle:
        for chunk in upload.chunks(CHUNK_SIZE):
            handle.write(chunk)
    return target


def make_exhibit_label(prefix="EX"):
    suffix = "".join(random.choices(string.ascii_uppercase + string.digits, k=4))
    return f"{prefix}-{suffix}"
