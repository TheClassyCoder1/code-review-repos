"""Hourly partitions for the audit-event tables.

Names come only from a timestamp and a fixed set of parent tables, never from
request data, so they can be interpolated into the DDL and information_schema lookup.
"""

import datetime

from opsdesk.part2.db_utils import table_exists

EVENT_TABLES = frozenset({"loan_audit_event", "account_audit_event", "risk_audit_event"})


def create_partition(cursor, parent: str, when: datetime.datetime) -> str:
    if parent not in EVENT_TABLES:
        raise ValueError("not a partitioned event table")
    start = when.astimezone(datetime.timezone.utc).replace(minute=0, second=0, microsecond=0)
    name = f"{parent}_{start.strftime('%Y%m%d_%H')}"
    if not table_exists(cursor, name):
        cursor.execute(
            f"CREATE TABLE IF NOT EXISTS {name} PARTITION OF {parent} FOR VALUES "
            f"FROM ('{start.isoformat()}') TO ('{(start + datetime.timedelta(hours=1)).isoformat()}');"
        )
    return name
