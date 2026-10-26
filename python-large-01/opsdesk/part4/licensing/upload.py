"""Entry point for installing an uploaded subscription manifest.

Everything the verifier reads before its signature check comes from the outer
archive, so the outer archive is bounded here: the encoded size, the member
count, and each member's declared uncompressed size (``ZipExtFile`` stops at
the size recorded in the central directory, so a member cannot inflate past
it). The inner ``consumer_export.zip`` is only opened after its signature has
been verified against the vendor CA, so its contents are trusted.
"""

import base64
import binascii
import io
import logging
import zipfile

from opsdesk.part4.licensing.manifest import validate_entitlement_manifest

logger = logging.getLogger(__name__)

MAX_MANIFEST_BYTES = 10 * 1024 * 1024
MAX_ENCODED_BYTES = (MAX_MANIFEST_BYTES + 2) // 3 * 4
MAX_OUTER_MEMBERS = 8
MAX_OUTER_MEMBER_BYTES = 8 * 1024 * 1024


class ManifestTooLarge(ValueError):
    pass


def check_manifest_bounds(data: str) -> None:
    if len(data) > MAX_ENCODED_BYTES:
        raise ManifestTooLarge("Invalid manifest: the manifest file is larger than 10 MiB.")
    try:
        raw = base64.b64decode(data, validate=True)
    except (binascii.Error, ValueError) as e:
        raise ValueError("Invalid manifest: the manifest is not valid base64.") from e
    try:
        outer = zipfile.ZipFile(io.BytesIO(raw))
    except zipfile.BadZipFile as e:
        raise ValueError("Invalid manifest: a subscription manifest zip file is required.") from e
    with outer:
        members = outer.infolist()
        if len(members) > MAX_OUTER_MEMBERS:
            raise ManifestTooLarge("Invalid manifest: unexpected files in the manifest.")
        for member in members:
            if member.file_size > MAX_OUTER_MEMBER_BYTES:
                raise ManifestTooLarge("Invalid manifest: a manifest member is too large.")


def install_manifest(user, data: str) -> list:
    """Validate ``data`` (base64 manifest zip) for an administrator and return its entitlements."""
    if not user.is_superuser:
        raise PermissionError("Only administrators may install a subscription.")
    check_manifest_bounds(data)
    subs = validate_entitlement_manifest(data)
    logger.info("subscription manifest installed by user %s (%d entitlements)", user.pk, len(subs))
    return subs
