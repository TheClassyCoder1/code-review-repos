"""Yandex Metrica traffic for the loan-application landing pages."""

import logging

import backoff
import requests
import yaml

from opsdesk.part2.query_runners.base import BaseQueryRunner

logger = logging.getLogger("opsdesk.part2.query_runners.yandex_metrica")


class QuotaException(Exception):
    pass


class YandexMetrica(BaseQueryRunner):
    url = "https://api-metrica.yandex.com"

    def run_query(self, query, user):
        response = self._send_query(**yaml.safe_load(query))
        return {"rows": response["data"], "totals": response.get("totals")}, None

    @backoff.on_exception(backoff.fibo, QuotaException, max_tries=10)
    def _send_query(self, path="stat/v1/data", **kwargs):
        token = kwargs.pop("oauth_token", self.configuration["token"])
        r = requests.get(
            f"{self.url}/{path}",
            headers={"Authorization": f"OAuth {token}"},
            params=kwargs,
        )

        response_data = r.json()

        if not r.ok:
            error_message = f"Code: {r.status_code}, message: {r.text}"
            if r.status_code == 429:
                logger.warning("Warning: 429 status code on Yandex Metrica query")
                raise QuotaException(error_message)
            raise Exception(error_message)
        return response_data
