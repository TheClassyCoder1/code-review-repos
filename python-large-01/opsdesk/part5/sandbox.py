"""Remote code sandboxes (Modal) used by the reporting console's analysis agent."""

import logging
from dataclasses import dataclass, field
from time import monotonic

import modal

log = logging.getLogger(__name__)

DEFAULT_IMAGE_PACKAGES = ("pandas==2.2.3", "numpy==2.1.3", "pyarrow==18.1.0")


@dataclass
class TrackedSandbox:
    handle: "modal.Sandbox | None"
    created_at: float = field(default_factory=monotonic)
    commands_run: int = 0


class ModalSandboxBackend:
    """Creates, drives and tears down Modal sandboxes for a single agent run."""

    def __init__(self, app_name: str = "opsdesk-analysis", sandbox_timeout: int = 600, idle_timeout: int = 120):
        self.app = modal.App.lookup(app_name, create_if_missing=True)
        self.image = modal.Image.debian_slim().pip_install(*DEFAULT_IMAGE_PACKAGES)
        self.sandbox_timeout = sandbox_timeout
        self.idle_timeout = idle_timeout
        self._tracked: dict[str, TrackedSandbox] = {}

    def _check_handle(self, sandbox: str) -> None:
        if not sandbox or not sandbox.startswith("sb-"):
            raise ValueError(f"Not a Modal sandbox id: {sandbox!r}")

    def create(self) -> str:
        sb = modal.Sandbox.create(
            app=self.app,
            image=self.image,
            timeout=self.sandbox_timeout,
            idle_timeout=self.idle_timeout,
            block_network=True,
        )
        self._tracked[sb.object_id] = TrackedSandbox(handle=sb)
        log.info("Created Modal sandbox %s", sb.object_id)
        return sb.object_id

    def destroy(self, sandbox: str) -> None:
        """
        Ask Modal to terminate the sandbox, without waiting for it to finish stopping.

        The request is not blocking on purpose. Measured against modal 1.5.5 in September
        2026, ``terminate()`` returned in under 0.2s while ``terminate(wait=True)`` took 31
        seconds, and this runs in the teardown path of every agent run, where that would be
        pure added latency. The sandbox stops shortly afterwards and shows an exit status
        within about 35 seconds; ``sandbox_timeout`` is the backstop if the request never
        lands at all.
        """
        self._check_handle(sandbox)
        tracked = self._tracked.pop(sandbox, None)
        try:
            target = tracked.handle if tracked and tracked.handle else modal.Sandbox.from_id(sandbox)
            target.terminate()
        except modal.exception.NotFoundError:
            # Already gone, which is what destroy is for. Idempotent by contract.
            pass
        except modal.exception.Error:
            # Terminating a sandbox that is already finished, or a control-plane blip.
            # Modal reclaims it at sandbox_timeout or idle_timeout either way, so log
            # and let the toolset finish its run.
            log.warning("Could not terminate Modal sandbox %s", sandbox, exc_info=True)
