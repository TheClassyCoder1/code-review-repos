import httpx
from celery import Celery
from celery.schedules import crontab
from celery.signals import worker_process_init, worker_process_shutdown

from payouts import config, db

celery = Celery("payouts", broker=config.REDIS_URL, backend=config.REDIS_URL)
celery.conf.update(
    task_acks_late=True,
    task_reject_on_worker_lost=True,
    worker_prefetch_multiplier=1,
    timezone="UTC",
    imports=("payouts.settlements.tasks",),
    beat_schedule={
        "weekly-settlements": {
            "task": "payouts.settlements.tasks.run_weekly_settlements",
            "schedule": crontab(hour=2, minute=0, day_of_week="mon"),
        },
        "resend-notifications": {
            "task": "payouts.settlements.tasks.resend_pending_notifications",
            "schedule": 300.0,
        },
    },
)

push_client = httpx.Client(
    base_url=config.PUSH_BASE_URL,
    timeout=httpx.Timeout(config.PUSH_TIMEOUT_SECONDS),
    headers={"User-Agent": "payouts-worker/1.0"},
)


@worker_process_init.connect
def _open_db(**_):
    db.open_pool()


@worker_process_shutdown.connect
def _close_resources(**_):
    push_client.close()
    db.close_pool()
