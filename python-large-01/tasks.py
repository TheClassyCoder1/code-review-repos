"""Release-management tasks for opsdesk (run with `invoke <task>`)."""

import os

from invoke import task


def run_command(context, command, **kwargs):
    with context.cd(os.path.dirname(os.path.abspath(__file__))):
        return context.run(command, echo=True, pty=False, **kwargs)


@task(help={"version": "The version number or the rule to update the version."})
def version(context, version=None):  # pylint: disable=redefined-outer-name
    """
    Show the version of Nautobot Python package or bump it when a valid bump rule is provided.

    The version number or rules are those supported by `poetry version`.
    """
    if version is None:
        version = ""

    run_command(context, f"poetry version --short {version}")
