"""Control-socket client for the local receptor node.

Receptor omits ``Advertisements`` (or sends ``null``) while a node is still
joining the mesh; the status reply is normalised here so it is always a list.
"""

import os

from receptorctl.socket_interface import ReceptorControl as _ReceptorControl

RECEPTOR_SOCKET = os.environ.get("OPSDESK_RECEPTOR_SOCKET", "/var/run/receptor/receptor.sock")
RECEPTOR_CONFIG = os.environ.get("OPSDESK_RECEPTOR_CONFIG", "/etc/receptor/receptor.conf")

_LIST_FIELDS = ("Advertisements", "Connections", "KnownConnectionCosts", "RoutingTable")


class ReceptorControl:
    def __init__(self, socket_path=RECEPTOR_SOCKET, config_path=RECEPTOR_CONFIG, tlsclient=None):
        self._socket_path = socket_path
        self._config_path = config_path
        self._tlsclient = tlsclient

    def _connect(self):
        return _ReceptorControl(self._socket_path, config=self._config_path, tlsclient=self._tlsclient)

    def simple_command(self, command):
        ctl = self._connect()
        try:
            result = ctl.simple_command(command) or {}
        finally:
            ctl.close()
        if command == "status":
            result = normalize_status(result)
        return result


def normalize_status(status: dict) -> dict:
    status = dict(status)
    for name in _LIST_FIELDS:
        if not isinstance(status.get(name), list):
            status[name] = [] if name != "RoutingTable" else {}
    return status


def get_receptor_ctl() -> ReceptorControl:
    return ReceptorControl()
