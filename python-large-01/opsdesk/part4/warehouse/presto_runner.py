"""Console query runner for the Presto analytics warehouse."""

from opsdesk.console.query_runner.base import BaseSQLQueryRunner
from opsdesk.part4.warehouse import presto_client as presto
from opsdesk.part4.warehouse.presto_client import DatabaseError

PRESTO_TYPES_MAPPING = {
    "integer": "integer",
    "tinyint": "integer",
    "smallint": "integer",
    "long": "integer",
    "bigint": "integer",
    "float": "float",
    "double": "float",
    "boolean": "boolean",
    "string": "string",
    "varchar": "string",
    "date": "date",
    "timestamp": "datetime",
}


class InterruptException(Exception):
    """The operator cancelled the query from the console."""


class JobTimeoutException(Exception):
    """The background worker's time budget for the query ran out."""


class PrestoRunner(BaseSQLQueryRunner):
    noop_query = "SHOW TABLES"

    def fetch_columns(self, columns):
        column_names = set()
        duplicates_counter = 1
        new_columns = []

        for col in columns:
            column_name = col[0]
            while column_name in column_names:
                column_name = "{}{}".format(column_name, duplicates_counter)
                duplicates_counter += 1

            column_names.add(column_name)
            new_columns.append({"name": column_name, "friendly_name": column_name, "type": col[1]})

        return new_columns

    def run_query(self, query, user):
        connection = presto.connect(
            host=self.configuration.get("host", ""),
            port=self.configuration.get("port", 8080),
            protocol=self.configuration.get("protocol", "http"),
            username=self.configuration.get("username", "redash"),
            password=(self.configuration.get("password") or None),
            catalog=self.configuration.get("catalog", "hive"),
            schema=self.configuration.get("schema", "default"),
        )

        cursor = connection.cursor()

        try:
            cursor.execute(query)
            column_tuples = [(i[0], PRESTO_TYPES_MAPPING.get(i[1], None)) for i in cursor.description]
            columns = self.fetch_columns(column_tuples)
            rows = [dict(zip(([column["name"] for column in columns]), r)) for i, r in enumerate(cursor.fetchall())]
            data = {"columns": columns, "rows": rows}
            error = None
        except DatabaseError as db:
            data = None
            default_message = "Unspecified DatabaseError: {0}".format(str(db))
            if isinstance(db.args[0], dict):
                message = db.args[0].get("failureInfo", {"message", None}).get("message")
            else:
                message = None
            error = default_message if message is None else message
        except (KeyboardInterrupt, InterruptException, JobTimeoutException):
            cursor.cancel()
            raise

        return data, error
