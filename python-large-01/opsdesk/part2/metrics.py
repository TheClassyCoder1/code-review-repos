"""Fire-and-forget statsd counters."""

import os
import socket

_ADDRESS = (os.environ.get("OPSDESK_STATSD_HOST", "127.0.0.1"), int(os.environ.get("OPSDESK_STATSD_PORT", "8125")))


class metrics:
    @staticmethod
    def send(name: str, kind: str, value):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.sendto(f"opsdesk.{name}:{value}|{'g' if kind == 'gauge' else 'c'}".encode(), _ADDRESS)
