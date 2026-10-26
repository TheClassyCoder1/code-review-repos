"""Cassandra data source (repayment event store)."""

import logging
import os
import ssl
import tempfile
from base64 import b64decode

from cassandra.auth import PlainTextAuthProvider
from cassandra.cluster import Cluster

from opsdesk.part2.query_runners.base import BaseQueryRunner

logger = logging.getLogger("opsdesk.part2.query_runners.cassandra")


class Cassandra(BaseQueryRunner):
    def _generate_cert_file(self):
        cert_encoded_bytes = self.configuration.get("sslCertificateFile", None)
        if not cert_encoded_bytes:
            return None
        with tempfile.NamedTemporaryFile(mode="w", delete=False) as cert_file:
            cert_file.write(b64decode(cert_encoded_bytes).decode("utf-8"))
        return cert_file.name

    def _cleanup_cert_file(self, cert_path):
        if cert_path:
            os.remove(cert_path)

    def _get_ssl_options(self, cert_path):
        if not self.configuration.get("useSsl", False):
            return None
        ssl_context = ssl.create_default_context(cafile=cert_path)
        return {"ssl_context": ssl_context}

    def run_query(self, query, user):
        connection = None
        cert_path = self._generate_cert_file()
        if self.configuration.get("username", "") and self.configuration.get("password", ""):
            auth_provider = PlainTextAuthProvider(
                username="{}".format(self.configuration.get("username", "")),
                password="{}".format(self.configuration.get("password", "")),
            )
            connection = Cluster(
                [self.configuration.get("host", "")],
                auth_provider=auth_provider,
                port=self.configuration.get("port", ""),
                protocol_version=self.configuration.get("protocol", 3),
                ssl_options=self._get_ssl_options(cert_path),
            )
        else:
            connection = Cluster(
                [self.configuration.get("host", "")],
                port=self.configuration.get("port", ""),
                protocol_version=self.configuration.get("protocol", 3),
                ssl_options=self._get_ssl_options(cert_path),
            )
        session = connection.connect()
        session.set_keyspace(self.configuration["keyspace"])
        session.default_timeout = self.configuration.get("timeout", 10)
        logger.debug("Cassandra running query: %s", query)
        result = session.execute(query)
        self._cleanup_cert_file(cert_path)

        column_names = result.column_names

        columns = self.fetch_columns([(c, "string") for c in column_names])

        rows = [dict(zip(column_names, row)) for row in result]

        data = {"columns": columns, "rows": rows}

        return data, None
