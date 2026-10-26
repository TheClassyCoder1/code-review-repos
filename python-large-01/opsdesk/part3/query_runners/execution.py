"""Runs console queries with a hard deadline.

SIGINT is ignored during `execute_query`, so runners never see
KeyboardInterrupt, and the deadline only starts when a runner calls
`arm_deadline()` after creating its client, so JobTimeoutException is only
raised while there is a connection to cancel.
"""

import signal


class JobTimeoutException(Exception):
    pass


def _expire(signum, frame):
    raise JobTimeoutException("query deadline exceeded")


_pending = {}


def arm_deadline() -> None:
    if "seconds" in _pending:
        signal.signal(signal.SIGALRM, _expire)
        signal.setitimer(signal.ITIMER_REAL, _pending.pop("seconds"))


def execute_query(runner, query: str, user, timeout: int = 300):
    previous_sigint = signal.signal(signal.SIGINT, signal.SIG_IGN)
    _pending["seconds"] = timeout
    try:
        return runner.run_query(query, user)
    finally:
        signal.setitimer(signal.ITIMER_REAL, 0)
        _pending.clear()
        signal.signal(signal.SIGINT, previous_sigint)
