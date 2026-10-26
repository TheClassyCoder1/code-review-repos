from typing import List

import requests
from zulip_bots.bots.trello import trello
from zulip_bots.bots.trello.trello import INVALID_ARGUMENTS_ERROR_MESSAGE, RESPONSE_ERROR_MESSAGE


class TrelloHandler(trello.TrelloHandler):
    def get_all_cards(self, content: List[str]) -> str:
        if len(content) != 2:
            return INVALID_ARGUMENTS_ERROR_MESSAGE

        board_id = content[1]
        get_cards_url = f"https://api.trello.com/1/boards/{board_id}/cards"
        cards_response = requests.get(get_cards_url, params=self.auth_params)

        try:
            cards = cards_response.json()
            bot_response = ["**Cards:**"]
            for index, card in enumerate(cards):
                bot_response += [
                    "{_count}. [{name}]({url}) (`{id}`)".format(_count=index + 1, **card)
                ]

        except (KeyError, ValueError, TypeError):
            return RESPONSE_ERROR_MESSAGE

        return "\n".join(bot_response)


handler_class = TrelloHandler
