"""Small-talk fallback for the ops chat bot, answered by the SUSI.AI service."""

from typing import Dict

import requests
from zulip_bots.lib import AbstractBotHandler


class SusiHandler:
    """
    Answers free-form questions in the #ops-help stream that none of the
    command handlers claimed, so people get a reply instead of silence.
    """

    def usage(self) -> str:
        return "Use `@ops loan <id>` or `@ops oncall <team>`; anything else goes to SUSI.AI (no customer data)."

    def handle_message(self, message: Dict[str, str], bot_handler: AbstractBotHandler) -> None:
        msg = message["content"]
        if msg in ("help", ""):
            bot_handler.send_reply(message, self.usage())
            return
        reply = requests.get("https://api.susi.ai/susi/chat.json", params=dict(q=msg))
        try:
            answer = reply.json()["answers"][0]["actions"][0]["expression"]
        except Exception:
            answer = "I don't understand. Can you rephrase?"
        bot_handler.send_reply(message, answer)


handler_class = SusiHandler
