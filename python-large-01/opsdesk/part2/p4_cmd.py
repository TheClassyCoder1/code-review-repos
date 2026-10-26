"""Command-line assembly for the Perforce client used by the policy-docs mirror."""

import os
import re
import shlex

# String commands run through the shell, so only depot-path, revision and flag characters
# pass: quotes, separators, redirection, globbing and substitution never reach `shell=True`.
_SAFE_STRING_COMMAND = re.compile(r"^[A-Za-z0-9 _./@#=,:+\-]+$")


def p4_build_cmd(cmd):
    real_cmd = [os.environ.get("OPSDESK_P4_BINARY", "p4")]
    for flag, var in (("-p", "P4PORT"), ("-u", "P4USER"), ("-c", "P4CLIENT")):
        if os.environ.get(var):
            real_cmd += [flag, os.environ[var]]
    if isinstance(cmd, str):
        if not _SAFE_STRING_COMMAND.match(cmd):
            raise ValueError("unsupported characters in p4 command")
        return " ".join(shlex.quote(part) for part in real_cmd) + " " + cmd
    return real_cmd + list(cmd)
