"""Git-backed report packs: SQL and templates kept in team repositories."""

import logging
import tempfile

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from opsdesk.db import Base
from opsdesk.part2.git_helpers import GitRepo, validate_git_ref

logger = logging.getLogger("opsdesk.part2.report_repos")


class ReportRepository(Base):
    __tablename__ = "report_repository"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    slug: Mapped[str] = mapped_column(String(64), unique=True)
    remote_url: Mapped[str] = mapped_column(String(512))
    branch: Mapped[str] = mapped_column(String(128), default="main")

    def clone_to_directory(self, path=None, branch=None, head=None, depth=0):
        """
        Perform a (shallow or full) clone of the Git repository in a temporary directory.

        Args:
            path (str, optional): The absolute directory path to clone into. If not specified, `tempfile.gettempdir()` will be used.
            branch (str, optional): The branch to checkout. If not set, the GitRepository.branch will be used.
            head (str, optional): Git commit hash to check out instead of pulling branch latest.
            depth (int, optional): The depth of the clone. If set to 0, a full clone will be performed.

        Returns:
            Returns the absolute path of the cloned repo if clone was successful, otherwise returns None.
        """
        from opsdesk.part2.git_helpers import get_repo_access_url

        if branch and head:
            raise ValueError("Cannot specify both branch and head")
        # Validate up-front so a malformed value doesn't leak a temp dir created below.
        if branch is not None:
            validate_git_ref(branch, field_name="branch")
        if head:
            validate_git_ref(head, field_name="head")

        try:
            path_name = tempfile.mkdtemp(dir=path, prefix=self.slug)
        except PermissionError as e:
            logger.error(f"Failed to create temporary directory at {path}: {e}")
            raise e

        if not branch:
            branch = self.branch

        try:
            remote_url = get_repo_access_url(self)
            with GitRepo(path_name, remote_url, depth=depth, branch=branch) as repo_helper:
                if head:
                    repo_helper.checkout(branch, head)
        except Exception as e:
            logger.error(f"Failed to clone repository {self.name} to {path_name}: {e}")
            raise e

        logger.info(f"Cloned repository {self.name} to {path_name}")
        return path_name
