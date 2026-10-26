"""HTTP client for user-supplied URLs: `advocate` (blocks private addresses) when installed."""

try:
    import advocate as requests_or_advocate
    from advocate.exceptions import UnacceptableAddressException
except ImportError:  # pragma: no cover - dev boxes without advocate
    import requests as requests_or_advocate

    class UnacceptableAddressException(Exception):
        pass
