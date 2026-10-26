"""Low-level helpers for the audit-event store (raw DB-API cursors)."""

import logging

logger = logging.getLogger("opsdesk.part2.db_utils")


def table_exists(cursor, table_name):
    cursor.execute(f"SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '{table_name}');")
    row = cursor.fetchone()
    if row is not None:
        for val in row:  # should only have 1
            if val is True:
                logger.debug(f'Event partition table {table_name} already exists')
                return True
    return False
