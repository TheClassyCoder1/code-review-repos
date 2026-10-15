import requests

from .config import settings


class TimeoutSession(requests.Session):
    def request(self, method, url, **kwargs):
        kwargs.setdefault("timeout", settings.http_timeout)
        return super().request(method, url, **kwargs)


session = TimeoutSession()
session.headers.update({"user-agent": "notify-worker/1.0"})
