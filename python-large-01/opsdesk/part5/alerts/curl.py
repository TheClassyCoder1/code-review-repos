"""Small HTTP client wrapper with the timeouts and error mapping alert transports need."""

from typing import Any

import requests

Params = dict[str, str] | None
Data = dict[str, Any] | str | bytes | None
Headers = dict[str, str] | None
Auth = tuple[str, str] | None
Response = requests.Response

DEFAULT_TIMEOUT = 10
USER_AGENT = "opsdesk-alerts/1.0"


class CurlError(Exception):
    def __init__(self, message: str) -> None:
        super().__init__(message)
        self.message = message


def request(method: str, url: str, *, headers: Headers = None, timeout: int = DEFAULT_TIMEOUT,
            **kwargs: Any) -> Response:
    """Send the request with our user agent; connection problems surface as ``CurlError``."""
    merged_headers = {"User-Agent": USER_AGENT, **(headers or {})}
    try:
        return requests.request(method, url, headers=merged_headers, timeout=timeout, **kwargs)
    except requests.Timeout as e:
        raise CurlError("Connection timed out") from e
    except requests.ConnectionError as e:
        raise CurlError("Connection failed") from e
