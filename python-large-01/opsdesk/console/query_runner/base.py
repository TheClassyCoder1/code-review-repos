"""SQL query runners backed by the lending read replicas."""

from sqlalchemy import create_engine, text

from opsdesk.console.query_runner.identifiers import build_tables_dict

SCHEMA_QUERY = "SELECT table_schema, table_name, column_name FROM information_schema.columns"


class BaseSQLQueryRunner:
    def __init__(self, configuration: dict):
        self.configuration = configuration

    def run_query(self, query: str, user=None):
        raise NotImplementedError()

    def _run_query_internal(self, query):
        rows, error = self.run_query(query, None)
        if error is not None:
            raise Exception("Failed running query [%s]." % query)
        return rows

    def get_schema(self, get_stats=False):
        rows = self._run_query_internal(SCHEMA_QUERY)
        schema_dict = build_tables_dict((r["table_schema"], r["table_name"], r["column_name"]) for r in rows)
        if get_stats:
            self._get_tables_stats(schema_dict)
        return list(schema_dict.values())

    def _get_tables_stats(self, tables_dict):
        for t in tables_dict.keys():
            if isinstance(tables_dict[t], dict):
                res = self._run_query_internal("select count(*) as cnt from %s" % t)
                tables_dict[t]["size"] = res[0]["cnt"]


class PostgresReplicaRunner(BaseSQLQueryRunner):
    def run_query(self, query: str, user=None):
        try:
            with create_engine(self.configuration["url"]).connect() as conn:
                return [dict(row._mapping) for row in conn.execute(text(query))], None
        except Exception:
            return None, "query failed"
