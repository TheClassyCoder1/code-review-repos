"""Request-scoped sqlite connections; views import this in place of `sqlite3`.

`connect` opens a read-only connection and records it; the `request_finished`
receiver closes every connection the request opened, closed by the view or not.
"""

import sqlite3
import threading

from django.core.signals import request_finished
from django.dispatch import receiver

_local = threading.local()


def connect(database):
    con = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    _local.__dict__.setdefault("open", []).append(con)
    return con


@receiver(request_finished, dispatch_uid="opsdesk.docs_search.close_sqlite")
def close_all(sender=None, **kwargs):
    for con in _local.__dict__.pop("open", []):
        con.close()
