"""Default timeout for `requests` in CLI tooling, which otherwise blocks forever on
`timeout=None`. `install()` wraps HTTPAdapter.send, which every requests call
goes through, so calls without their own timeout get DEFAULT_TIMEOUT.
"""

from requests.adapters import HTTPAdapter

DEFAULT_TIMEOUT = (5, 60)  # (connect, read) seconds


def install(timeout=DEFAULT_TIMEOUT):
    original_send = HTTPAdapter.send

    def send(self, request, **kwargs):  # Session.send passes adapter arguments by keyword
        kwargs["timeout"] = kwargs.get("timeout") or timeout
        return original_send(self, request, **kwargs)

    if not getattr(original_send, "with_default_timeout", False):
        send.with_default_timeout = True
        HTTPAdapter.send = send
