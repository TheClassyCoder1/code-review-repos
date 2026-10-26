"""Git plumbing for report-pack repositories."""

import os
import re
import subprocess

_GIT_REF = re.compile(r"^(?!-)(?!.*\.\.)[A-Za-z0-9._/\-]{1,200}$")


def validate_git_ref(value: str, field_name: str = "ref") -> str:
    if not _GIT_REF.match(value or ""):
        raise ValueError(f"Invalid {field_name}: must be a plain branch name or commit hash")
    return value


def get_repo_access_url(repository) -> str:
    return os.environ.get(f"OPSDESK_REPO_URL_{repository.slug.upper()}", repository.remote_url)


class GitRepo:
    """Clone `remote_url` into `path` on enter; the checkout is left in place."""

    def __init__(self, path: str, remote_url: str, depth: int = 0, branch: str | None = None):
        self.path, self.remote_url, self.depth, self.branch = path, remote_url, depth, branch

    def _git(self, *args):
        subprocess.run(["git", *args], check=True, capture_output=True, timeout=300)

    def __enter__(self):
        self._git("clone", "--quiet", f"--depth={int(self.depth)}" if self.depth else "--no-single-branch",
                  f"--branch={self.branch}", "--", self.remote_url, self.path)
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def checkout(self, branch: str, head: str):
        self._git("-C", self.path, "checkout", "--quiet", "--detach", head)
