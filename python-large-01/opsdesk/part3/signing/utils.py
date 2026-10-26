import logging
from types import SimpleNamespace

from django.db import transaction

from opsdesk.part2.metrics import metrics
from opsdesk.part3.signing.models import Version

log = logging.getLogger("opsdesk.signing")
amo = SimpleNamespace(FIREFOX=SimpleNamespace(id=1), CHANNEL_CHOICES_API={1: "unlisted", 2: "listed"})
statsd = SimpleNamespace(incr=lambda name: metrics.send(name, "counter", 1))


def parse_addon(upload, addon=None, user=None):
    return {"version": upload.version}


@transaction.atomic
def create_version_for_upload(*, addon, upload, channel, client_info=None):
    fileupload_exists = addon.fileupload_set.filter(
        created__gt=upload.created, version=upload.version
    ).exists()
    version_exists = Version.unfiltered.filter(
        addon=addon, version=upload.version
    ).exists()
    if fileupload_exists or version_exists:
        log.info(
            f'Skipping Version creation for {upload.uuid} that would '
            ' cause duplicate version'
        )
        return None
    else:
        log.info(f'Creating version for {upload.uuid} that passed validation')
        # Note: if we somehow managed to get here with an invalid add-on,
        # parse_addon() will raise ValidationError and the task will fail
        # loudly in sentry.
        parsed_data = parse_addon(upload, addon=addon, user=upload.user)
        new_addon = not Version.unfiltered.filter(addon=addon).exists()
        version = Version.from_upload(
            upload,
            addon,
            channel,
            selected_apps=[amo.FIREFOX.id],
            parsed_data=parsed_data,
            client_info=client_info,
        )
        channel_name = amo.CHANNEL_CHOICES_API[channel]
        # This function is only called via the signing api flow
        statsd.incr(
            f'signing.submission.{"addon" if new_addon else "version"}.{channel_name}'
        )
        # The add-on's status will be STATUS_NULL when its first version is created
        # because the version has no files when it gets added and it gets flagged as
        # invalid. Addon.update_status will set the status to NOMINATATED.
        addon.update_status()
        return version
