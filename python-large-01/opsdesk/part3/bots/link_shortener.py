from typing import Any

import requests
from zulip_bots.bots.link_shortener import link_shortener


class LinkShortenerHandler(link_shortener.LinkShortenerHandler):
    def call_link_shorten_service(self, long_url: str) -> Any:
        response = requests.get(
            "https://api-ssl.bitly.com/v3/shorten",
            params={"access_token": self.config_info["key"], "longUrl": long_url},
        )
        return response.json()


handler_class = LinkShortenerHandler
