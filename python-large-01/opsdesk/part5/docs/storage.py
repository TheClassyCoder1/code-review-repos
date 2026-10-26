"""Moves built documentation artifacts between the builder and the serving bucket."""

import logging

from django.core.files.storage import storages

log = logging.getLogger(__name__)

BUILD_MEDIA_ALIAS = "build-media"
BUILD_TOOLS_ALIAS = "build-tools"


def _get_storage_backend_class(alias: str):
    """
    Get the storage backend class for a given alias from STORAGES setting.

    This returns the class, not an instance, so it can be instantiated
    with custom kwargs (e.g., per-build credentials).
    """
    storage = storages[alias]
    return type(storage)


def get_build_media_storage(credentials: dict | None = None):
    """Return the artifact storage, optionally bound to short-lived per-build credentials."""
    if not credentials:
        return storages[BUILD_MEDIA_ALIAS]
    backend_class = _get_storage_backend_class(BUILD_MEDIA_ALIAS)
    return backend_class(
        access_key=credentials["access_key_id"],
        secret_key=credentials["secret_access_key"],
        security_token=credentials.get("session_token"),
    )
