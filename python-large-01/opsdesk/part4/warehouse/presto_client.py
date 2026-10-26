"""Minimal DB-API style client for the Presto/Trino REST protocol.

``execute`` POSTs the statement and follows ``nextUri`` to the end inside a
short-lived ``requests.Session``, so a connection only holds configuration and
a cursor only holds the buffered result. No socket or server-side query stays
open once ``execute`` returns, which is why ``close`` on both is a no-op.
"""

import base64
import logging

import requests

logger = logging.getLogger(__name__)

DEFAULT_TIMEOUT = (5, 300)
MAX_RESULT_ROWS = 250_000


class Error(Exception):
    pass


class DatabaseError(Error):
    """Raised with the coordinator's ``error`` object as ``args[0]`` when it sent one."""


class OperationalError(DatabaseError):
    pass


def _error_info(message: str) -> dict:
    return {"message": message, "failureInfo": {"message": message}}


class PrestoConnection:
    def __init__(self, host, port, protocol, username, password, catalog, schema):
        self.base_url = "{}://{}:{}".format(protocol, host, port)
        self.username = username
        self.password = password
        self.catalog = catalog
        self.schema = schema

    def headers(self) -> dict:
        headers = {
            "X-Presto-User": self.username,
            "X-Presto-Catalog": self.catalog,
            "X-Presto-Schema": self.schema,
            "X-Presto-Source": "opsdesk",
        }
        if self.password:
            token = base64.b64encode("{}:{}".format(self.username, self.password).encode()).decode()
            headers["Authorization"] = "Basic " + token
        return headers

    def cursor(self) -> "PrestoCursor":
        return PrestoCursor(self)

    def close(self):
        # Nothing is held open between statements; see the module docstring.
        return None


class PrestoCursor:
    def __init__(self, connection: PrestoConnection):
        self._connection = connection
        self._rows: list = []
        self._columns: list | None = None
        self._query_id: str | None = None
        self._next_uri: str | None = None

    @property
    def description(self):
        if self._columns is None:
            return None
        return [(c["name"], c["type"], None, None, None, None, True) for c in self._columns]

    def execute(self, operation, parameters=None):
        if parameters:
            raise Error("parameter binding is not supported by the warehouse client")
        self._rows, self._columns = [], None
        with requests.Session() as http:
            http.headers.update(self._connection.headers())
            response = http.post(
                self._connection.base_url + "/v1/statement",
                data=operation.encode("utf-8"),
                timeout=DEFAULT_TIMEOUT,
            )
            payload = self._check(response)
            self._query_id = payload.get("id")
            while True:
                self._consume(payload)
                self._next_uri = payload.get("nextUri")
                if self._next_uri is None:
                    break
                if len(self._rows) > MAX_RESULT_ROWS:
                    http.delete(self._next_uri, timeout=DEFAULT_TIMEOUT)
                    self._next_uri = None
                    raise OperationalError(_error_info("Result exceeds {} rows".format(MAX_RESULT_ROWS)))
                payload = self._check(http.get(self._next_uri, timeout=DEFAULT_TIMEOUT))
        self._next_uri = None
        logger.debug("warehouse query %s drained (%d rows)", self._query_id, len(self._rows))

    def _check(self, response) -> dict:
        if response.status_code == 503:
            raise OperationalError(_error_info("warehouse coordinator unavailable"))
        if response.status_code != 200:
            raise OperationalError(_error_info("unexpected status {}".format(response.status_code)))
        payload = response.json()
        if payload.get("error"):
            error = dict(payload["error"])
            error.setdefault("failureInfo", {"message": error.get("message")})
            raise DatabaseError(error)
        state = payload.get("stats", {}).get("state")
        if state == "CANCELED":
            raise DatabaseError(_error_info("query was canceled"))
        return payload

    def _consume(self, payload: dict):
        if self._columns is None and payload.get("columns"):
            self._columns = payload["columns"]
        self._rows.extend(payload.get("data") or [])

    def fetchall(self):
        rows, self._rows = self._rows, []
        return rows

    def cancel(self):
        # A statement is fully drained inside ``execute``; a cancel can only
        # arrive mid-drain, in which case the pending page is released.
        if self._next_uri:
            requests.delete(self._next_uri, headers=self._connection.headers(), timeout=DEFAULT_TIMEOUT)
            self._next_uri = None

    def close(self):
        return None


def connect(host, port=8080, protocol="http", username="opsdesk", password=None, catalog="hive", schema="default"):
    if protocol not in ("http", "https"):
        raise ValueError("protocol must be http or https")
    if password and protocol != "https":
        raise ValueError("password authentication requires https")
    return PrestoConnection(host, port, protocol, username, password, catalog, schema)
