"""Query runners for the console's saved reports."""

from opsdesk.console.query_runner.base import PostgresReplicaRunner
from opsdesk.console.query_runner.script import ScriptRunner

RUNNERS = {"pg_replica": PostgresReplicaRunner, "script": ScriptRunner}


def get_runner(kind: str, configuration: dict):
    return RUNNERS[kind](configuration)
