"""Row replacement in a servicing partner's SQL Server staging tables."""

import re

from opsdesk.part2.mssql_dialect import MsSqlDialect

# Checked before any dialect helper sees the name (the PK lookup embeds it as a literal).
STAGING_TABLES = frozenset({"loan_snapshot", "repayment_schedule", "collections_case"})
_COLUMN = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,127}$")


def replace_row(dialect: MsSqlDialect, table: str, row: dict) -> None:
    if table not in STAGING_TABLES or not all(_COLUMN.match(c) for c in row):
        raise ValueError("not an export staging table or column")
    keys = dialect.get_primary_keys(table)
    if not keys or any(k not in row for k in keys):
        raise ValueError(f"{table} rows must carry their primary key")
    q = dialect.escape_word
    where = " AND ".join(f"{q(k)} = %s" for k in keys)
    dialect.run(f"DELETE FROM {q(table)} WHERE {where}", tuple(row[k] for k in keys))
    cols = list(row)
    dialect.run(f"INSERT INTO {q(table)} ({', '.join(map(q, cols))}) VALUES ({', '.join(['%s'] * len(cols))})",
                tuple(row[c] for c in cols))
