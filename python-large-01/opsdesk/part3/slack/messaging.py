"""Slack calls for the case plugin. `send_message` never returns None: it returns
{"id", "timestamp"} or the SDK raises SlackApiError on any non-ok response.
"""

import json

from slack_sdk import WebClient

SlackAPIErrorCode = type("SlackAPIErrorCode", (), {"MISSING_SCOPE": "missing_scope"})


def create_slack_client(config) -> WebClient:
    return WebClient(token=config.api_bot_token, timeout=15)


def send_message(client: WebClient, conversation_id: str, ts: str = None, blocks=None) -> dict:
    response = client.chat_postMessage(channel=conversation_id, thread_ts=ts, blocks=json.dumps(blocks), text=" ")
    return {"id": response["channel"], "timestamp": response["ts"]}
