"""Query runner that reads the cached results of other saved queries.

Lets an operator write ``SELECT ... FROM query_42`` to join the output of
saved report queries without touching the source databases again.
"""

import re

from opsdesk.part4.warehouse import models
from opsdesk.part4.warehouse.models import has_access, view_only

QUERY_REFERENCE = re.compile(r"\bquery_(\d+)\b", re.IGNORECASE)
MAX_REFERENCED_QUERIES = 20


def extract_query_ids(query: str) -> list[int]:
    return list(dict.fromkeys(int(m) for m in QUERY_REFERENCE.findall(query)))


def _load_query(user, query_id):
    query = models.Query.get_by_id(query_id)

    if user.org_id != query.org_id:
        raise PermissionError("Query id {} not found.".format(query.id))

    # TODO: this duplicates some of the logic we already have in the redash.handlers.query_results.
    # We should merge it so it's consistent.
    if not has_access(query.data_source, user, view_only):
        raise PermissionError("You do not have access to query id {}.".format(query.id))

    return query


def load_referenced_queries(user, query: str) -> list:
    query_ids = extract_query_ids(query)
    if len(query_ids) > MAX_REFERENCED_QUERIES:
        raise ValueError("A query may reference at most {} saved queries.".format(MAX_REFERENCED_QUERIES))
    return [_load_query(user, query_id) for query_id in query_ids]
