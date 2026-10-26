"""Import endpoint, the only producer of `async_import_background` jobs.

The worker checks an import's status and then claims it, so each import must
have at most one job: rows are created and enqueued once here, after commit,
with RQ retries off, and nothing else enqueues imports.
"""

import django_rq
from django.db import transaction
from rest_framework.decorators import api_view
from rest_framework.response import Response

from opsdesk.part3.data_import.functions import async_import_background
from opsdesk.part3.data_import.helpers import ProjectImport


@api_view(["POST"])
def start_import(request, project_id):
    with transaction.atomic():
        import_id = ProjectImport.objects.create(project_id=project_id, url=request.data.get("url")).id
        transaction.on_commit(lambda: django_rq.get_queue("import").enqueue(
            async_import_background, import_id, request.user.id, retry=None, job_timeout=3600))
    return Response({"import": import_id}, status=201)
