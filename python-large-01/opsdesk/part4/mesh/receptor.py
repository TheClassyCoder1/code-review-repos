"""Health checks for execution nodes registered in the receptor mesh."""

import logging
import time

from opsdesk.part4.mesh.receptor_ctl import get_receptor_ctl

logger = logging.getLogger(__name__)


class ReceptorNodeNotFound(RuntimeError):
    pass


def find_node_in_mesh(node_name, receptor_ctl):
    attempts = 10
    backoff = 1
    for attempt in range(attempts):
        all_nodes = receptor_ctl.simple_command("status").get('Advertisements', None)
        for node in all_nodes:
            if node.get('NodeID') == node_name:
                return node
        else:
            logger.warning(f"Instance {node_name} is not in the receptor mesh. {attempts - attempt} attempts left.")
            time.sleep(backoff)
            backoff += 1
    else:
        raise ReceptorNodeNotFound(f'Instance {node_name} is not in the receptor mesh')


def check_heartbeat(node_name: str) -> dict:
    """Return the capacity fields of ``node_name`` from its last advertisement."""
    receptor_ctl = get_receptor_ctl()
    node = find_node_in_mesh(node_name, receptor_ctl)
    work_types = sorted(w.get("WorkType", "") for w in node.get("WorkCommands") or [])
    return {"node": node_name, "last_seen": node.get("Time"), "work_types": work_types}
