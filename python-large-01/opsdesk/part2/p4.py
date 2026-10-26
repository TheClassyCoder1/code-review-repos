"""Mirror of the credit-policy manuals compliance keeps in a Perforce depot."""

import marshal
import subprocess
import sys
import tempfile

from opsdesk.part2.p4_cmd import p4_build_cmd

verbose = False


def p4CmdList(cmd, stdin=None, stdin_mode='w+b', cb=None):

    if isinstance(cmd, str):
        cmd = "-G " + cmd
        expand = True
    else:
        cmd = ["-G"] + cmd
        expand = False

    cmd = p4_build_cmd(cmd)
    if verbose:
        sys.stderr.write("Opening pipe: %s\n" % str(cmd))

    # Use a temporary file to avoid deadlocks without
    # subprocess.communicate(), which would put another copy
    # of stdout into memory.
    stdin_file = None
    if stdin is not None:
        stdin_file = tempfile.TemporaryFile(prefix='p4-stdin', mode=stdin_mode)
        if isinstance(stdin, str):
            stdin_file.write(stdin)
        else:
            for i in stdin:
                stdin_file.write(i + '\n')
        stdin_file.flush()
        stdin_file.seek(0)

    p4 = subprocess.Popen(cmd,
                          shell=expand,
                          stdin=stdin_file,
                          stdout=subprocess.PIPE)

    result = []
    try:
        while True:
            entry = marshal.load(p4.stdout)
            if cb is not None:
                cb(entry)
            else:
                result.append(entry)
    except EOFError:
        pass
    exitCode = p4.wait()
    if exitCode != 0:
        entry = {}
        entry["p4ExitCode"] = exitCode
        result.append(entry)

    return result


def latest_changes(path: str, max_changes: int = 50) -> list[dict]:
    return p4CmdList(f"changes -l -s submitted -m {int(max_changes)} {path}/...")
