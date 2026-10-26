import logging

from opsdesk.part2.query_runners.base import BaseQueryRunner
from opsdesk.part3.query_runners.execution import JobTimeoutException, arm_deadline

logger = logging.getLogger(__name__)


class Ignite(BaseQueryRunner):
    def server_to_connection(self, s):
        arm_deadline()  # the client exists by now, so a deadline expiry can cancel it
        host, _, port = s.partition(":")
        return (host, int(port or 10800))

    def _parse_results(self, c):
        names = next(c)
        return self.fetch_columns([(n, "string") for n in names]), [dict(zip(names, row)) for row in c]

    def run_query(self, query, user):
        connection = None

        try:
            server = self.configuration.get("server", "127.0.0.1:10800")
            user = self.configuration.get("user", None)
            password = self.configuration.get("password", None)
            tls = self.configuration.get("tls", False)
            distributed_joins = self.configuration.get("distributed_joins", False)
            enforce_join_order = self.configuration.get("enforce_join_order", False)
            lazy = self.configuration.get("lazy", True)
            gridgain = self.configuration.get("gridgain", False)

            if gridgain:
                from pygridgain import Client
            else:
                from pyignite import Client

            connection = Client(username=user, password=password, use_ssl=tls)
            connection.connect([self.server_to_connection(s) for s in server.split(",")])

            cursor = connection.sql(
                query,
                include_field_names=True,
                distributed_joins=distributed_joins,
                enforce_join_order=enforce_join_order,
                lazy=lazy,
            )
            logger.debug("Ignite running query: %s", query)

            result = self._parse_results(cursor)
            data = {"columns": result[0], "rows": result[1]}
            error = None

        except (KeyboardInterrupt, JobTimeoutException):
            connection.cancel()
            raise
        finally:
            if connection:
                connection.close()

        return data, error
