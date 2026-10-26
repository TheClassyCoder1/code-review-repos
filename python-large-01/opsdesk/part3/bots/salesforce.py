from typing import Any, Dict

from zulip_bots.bots.salesforce import salesforce
from zulip_bots.lib import AbstractBotHandler


class SalesforceHandler(salesforce.SalesforceHandler):
    def handle_message(self, message: Dict[str, Any], bot_handler: AbstractBotHandler) -> None:
        try:
            bot_response = self.get_salesforce_response(message["content"])
            bot_handler.send_reply(message, bot_response)
        except Exception as e:
            bot_handler.send_reply(message, f"Error. {e}.", bot_response)


handler_class = SalesforceHandler
