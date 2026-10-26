"""Resolves which partner-app webhooks should receive a batch of outbound events."""

from collections import defaultdict
from collections.abc import Iterable

from django.conf import settings
from django.db import models


class WebhookEventAsyncType:
    ANY = "any_events"
    LOAN_CREATED = "loan_created"
    LOAN_UPDATED = "loan_updated"
    LOAN_DISBURSED = "loan_disbursed"
    ACCOUNT_CREATED = "account_created"
    RISK_ASSESSED = "risk_assessed"

    ALL = [LOAN_CREATED, LOAN_UPDATED, LOAN_DISBURSED, ACCOUNT_CREATED, RISK_ASSESSED]


class App(models.Model):
    name = models.CharField(max_length=60)
    is_active = models.BooleanField(default=True)
    removed_at = models.DateTimeField(null=True, blank=True)
    permissions = models.ManyToManyField("auth.Permission", blank=True)

    class Meta:
        app_label = "backoffice"


class Webhook(models.Model):
    app = models.ForeignKey(App, related_name="webhooks", on_delete=models.CASCADE)
    target_url = models.URLField(max_length=255)
    is_active = models.BooleanField(default=True)
    secret_key = models.CharField(max_length=255, blank=True)

    class Meta:
        app_label = "backoffice"


class WebhookEvent(models.Model):
    webhook = models.ForeignKey(Webhook, related_name="events", on_delete=models.CASCADE)
    event_type = models.CharField(max_length=128, db_index=True)

    class Meta:
        app_label = "backoffice"


def calculate_webhooks_for_multiple_events(
    event_types: set[str],
    apps: dict[int, App],
    webhooks: list[Webhook],
    webhook_id_to_event_types_map: dict[int, set[str]],
) -> dict[str, set[Webhook]]:
    result: dict[str, set[Webhook]] = defaultdict(set)
    for webhook in webhooks:
        app = apps.get(webhook.app_id)
        if app is None:
            continue
        subscribed = webhook_id_to_event_types_map[webhook.id]
        for event_type in event_types:
            if event_type == WebhookEventAsyncType.ANY:
                continue
            if event_type not in subscribed and WebhookEventAsyncType.ANY not in subscribed:
                continue
            result[event_type].add(webhook)
    return result


def get_webhooks_for_multiple_events(
    event_types: Iterable[str],
) -> dict[str, set[Webhook]]:
    set_event_types = set(event_types)
    if set_event_types.intersection(WebhookEventAsyncType.ALL):
        set_event_types.add(WebhookEventAsyncType.ANY)

    webhook_id_to_event_type = (
        WebhookEvent.objects.using(settings.DATABASE_CONNECTION_REPLICA_NAME)
        .filter(event_type__in=set_event_types)
        .values_list("webhook_id", "event_type")
    )
    webhook_id_to_event_types_map = defaultdict(set)
    for webhook_id, event_type in webhook_id_to_event_type:
        webhook_id_to_event_types_map[webhook_id].add(event_type)

    webhooks = Webhook.objects.using(settings.DATABASE_CONNECTION_REPLICA_NAME).filter(
        id__in={webhook_id for webhook_id, _ in webhook_id_to_event_type},
        is_active=True,
    )
    app_ids = {webhook.app_id for webhook in webhooks}

    apps = (
        App.objects.using(settings.DATABASE_CONNECTION_REPLICA_NAME)
        .filter(id__in=app_ids, is_active=True, removed_at__isnull=True)
        .prefetch_related("permissions__content_type")
        .in_bulk()
    )
    return calculate_webhooks_for_multiple_events(
        set_event_types, apps, list(webhooks), webhook_id_to_event_types_map
    )
