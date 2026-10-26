"""Runs operator-maintained report scripts and returns their JSON output."""

import json
import os
import subprocess

from opsdesk.console import config


def query_to_script_path(path, query):
    if path != "*":
        script = os.path.join(path, query.split(" ")[0])
        if not os.path.exists(script):
            raise IOError("Script '{}' not found in script directory".format(query))

        return os.path.join(path, query).split(" ")

    return query


def run_script(script, shell):
    output = subprocess.check_output(script, shell=shell, timeout=120)
    if output is None:
        return None, "Error reading output"
    output = output.decode("utf-8").strip()
    if not output:
        return None, "Empty output from script"
    return json.loads(output), None


class ScriptRunner:
    def __init__(self, configuration: dict):
        self.configuration = configuration
        self.path = configuration.get("path") or config.QUERY_SCRIPTS_DIR
        self.shell = bool(configuration.get("shell", False))

    def run_query(self, query, user=None):
        try:
            return run_script(query_to_script_path(self.path, query), self.shell)
        except (IOError, subprocess.SubprocessError) as e:
            return None, str(e)
