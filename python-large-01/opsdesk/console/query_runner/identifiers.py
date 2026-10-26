"""Introspected table names end up in statistics queries, so only plain or
schema-qualified SQL identifiers are kept, each part double-quoted."""

import re

_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_$]{0,62}$")


def quote_qualified_name(name: str) -> str | None:
    parts = name.split(".")
    if len(parts) > 2 or not all(_IDENTIFIER.match(part) for part in parts):
        return None
    return ".".join('"%s"' % part for part in parts)


def build_tables_dict(rows) -> dict:
    """Group ``(schema, table, column)`` rows; rows with unsafe names are dropped."""
    tables: dict = {}
    for schema_name, table_name, column_name in rows:
        qualified = table_name if schema_name in ("public", None) else "%s.%s" % (schema_name, table_name)
        quoted = quote_qualified_name(qualified)
        if quoted is None:
            continue
        tables.setdefault(quoted, {"name": qualified, "columns": []})["columns"].append(column_name)
    return tables
