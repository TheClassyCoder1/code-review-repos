"""Hands out review tasks. `Task.set_lock` counts locks then writes one, so it
only runs here, after SELECT ... FOR UPDATE on the task in the same transaction:
concurrent reviewers serialize on the row and the second sees the first's lock.
"""

import logging

from django.db import transaction

from opsdesk.part3.labeling.models import Task

get_next_task_logging_level = lambda user: logging.INFO if user.is_staff else logging.DEBUG  # noqa: E731


def acquire_task(project, user):
    for task_id in Task.objects.filter(project=project).values_list("id", flat=True)[:20]:
        with transaction.atomic():
            task = Task.objects.select_for_update().select_related("project").get(pk=task_id)
            if task.num_locks < task.overlap:
                task.set_lock(user)
                return task
