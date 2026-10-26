import datetime
import logging
import os

from django.conf import settings
from django.db import models
from django.utils.timezone import now

logger = logging.getLogger(__name__)


flag_set = lambda name, user=None: name in os.environ.get("OPSDESK_FLAGS", "").split(",")  # noqa: E731


class ReviewProject(models.Model):
    custom_task_lock_ttl = models.PositiveIntegerField(null=True)
    label_config = models.TextField(default="")
    label_config_is_not_default = property(lambda self: bool(self.label_config))


class Task(models.Model):
    project = models.ForeignKey(ReviewProject, on_delete=models.CASCADE)
    overlap = models.PositiveSmallIntegerField(default=1)
    num_locks = property(lambda self: self.locks.filter(expire_at__gt=now()).count())

    def clear_expired_locks(self):
        self.locks.filter(expire_at__lt=now()).delete()

    def set_lock(self, user):
        """Lock current task by specified user. Lock lifetime is set by `expire_in_secs`"""
        from opsdesk.part3.labeling.next_task import get_next_task_logging_level

        num_locks = self.num_locks
        if num_locks < self.overlap:
            lock_ttl = settings.TASK_LOCK_TTL
            if (
                flag_set('fflag_feat_all_leap_1534_custom_task_lock_timeout_short', user=user)
                and self.project.custom_task_lock_ttl
            ):
                lock_ttl = self.project.custom_task_lock_ttl
            expire_at = now() + datetime.timedelta(seconds=lock_ttl)
            try:
                task_lock = TaskLock.objects.get(task=self, user=user)
            except TaskLock.DoesNotExist:
                TaskLock.objects.create(task=self, user=user, expire_at=expire_at)
            else:
                task_lock.expire_at = expire_at
                task_lock.save()
            logger.log(
                get_next_task_logging_level(user),
                f'User={user} acquires a lock for the task={self} ttl: {lock_ttl}',
            )
        else:
            logger.error(
                f'Current number of locks for task {self.id} is {num_locks}, but overlap={self.overlap}: '
                f"that's a bug because this task should not be taken in a label stream (task should be locked)"
            )
        self.clear_expired_locks()


class TaskLock(models.Model):
    task = models.ForeignKey(Task, related_name="locks", on_delete=models.CASCADE)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    expire_at = models.DateTimeField()
