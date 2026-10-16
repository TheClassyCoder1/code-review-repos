import logging

import httpx

log = logging.getLogger(__name__)


def send_push(client, driver_id, title, body, data=None):
    response = client.post(
        "/v1/push",
        json={
            "audience": {"driver_id": driver_id},
            "title": title,
            "body": body,
            "data": data or {},
        },
    )
    response.raise_for_status()
    return response.json().get("message_id")


def format_amount(amount, currency="EUR"):
    return f"{currency} {amount:,.2f}"


def settlement_message(settlement):
    amount = format_amount(settlement["net_total"])
    week = settlement["week_start"].strftime("%d %b")
    return "Weekly payout sent", f"{amount} for the week of {week} is on its way."


def notify_settlement_ready(client, notification):
    title, body = settlement_message(notification["payload"])
    try:
        return send_push(
            client,
            notification["driver_id"],
            title,
            body,
            data={"settlement_id": notification["payload"]["id"]},
        )
    except httpx.HTTPError as exc:
        log.warning(
            "push failed notification_id=%s driver_id=%s error=%s",
            notification["id"],
            notification["driver_id"],
            type(exc).__name__,
        )
        return None
