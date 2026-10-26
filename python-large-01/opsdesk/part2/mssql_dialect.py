"""SQL Server dialect for the servicing-partner export hook."""

import logging
from functools import lru_cache

import pymssql


def fetch_all_handler(cursor):
    return cursor.fetchall() if cursor.description is not None else None


class MsSqlDialect:
    def __init__(self, conn_params: dict):
        self.conn_params = conn_params
        self.log = logging.getLogger("opsdesk.part2.mssql")

    def run(self, sql: str, parameters=None, handler=None):
        with pymssql.connect(**self.conn_params) as conn, conn.cursor() as cursor:
            cursor.execute(sql, parameters)
            result = handler(cursor) if handler is not None else None
            conn.commit()
        return result

    def escape_word(self, word: str) -> str:
        return "[" + word.replace("]", "]]") + "]"

    @lru_cache(maxsize=None)
    def get_primary_keys(self, table: str, schema: str | None = None) -> list[str] | None:
        primary_keys = self.run(
            f"""
                SELECT c.name
                FROM sys.columns c
                WHERE c.object_id =  OBJECT_ID('{table}')
                    AND EXISTS (SELECT 1 FROM sys.index_columns ic
                        INNER JOIN sys.indexes i ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                        WHERE i.is_primary_key = 1
                        AND ic.object_id = c.object_id
                        AND ic.column_id = c.column_id);
                """,
            handler=fetch_all_handler,
        )
        primary_keys = [pk[0] for pk in primary_keys] if primary_keys else []
        self.log.debug("Primary keys for table '%s': %s", table, primary_keys)
        return primary_keys  # type: ignore
