"""Background jobs for the knowledge base."""

import logging

from celery import shared_task

log = logging.getLogger(__name__)


@shared_task
def translate_revision(payload):
    from .models import Revision

    revision = Revision.objects.get(pk=payload["revision_id"])
    log.info(
        "Translating revision %s into %s via %s",
        revision.pk,
        payload["target_locale"],
        payload["method"],
    )


@shared_task
def add_short_links(doc_ids):
    from .models import Document

    for doc in Document.objects.filter(pk__in=doc_ids, share_link=""):
        doc.share_link = "/kb/s/{:x}".format(doc.pk)
        doc.save(update_fields=["share_link"])
