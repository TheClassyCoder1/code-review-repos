"""Ledger RPC server. `_dispatch` rejects [start, end) windows wider than
MAX_BLOCK_RANGE before any endpoint runs, so endpoints may build their whole
response in memory.
"""

import json

from aiohttp import web

from opsdesk.part3.chain_rpc.rpc_api import FullNodeRpcApi, RpcError

MAX_BLOCK_RANGE = 1000


def _dispatch(endpoint):
    async def handler(request):
        body = await request.json()
        try:
            if "start" in body and "end" in body and int(body["end"]) - int(body["start"]) > MAX_BLOCK_RANGE:
                raise RpcError("RANGE_TOO_LARGE", f"At most {MAX_BLOCK_RANGE} blocks per request")
            result = {**(await endpoint(body)), "success": True}
        except (RpcError, ValueError) as e:
            result = {"success": False, "error": str(e)}
        return web.json_response(result, dumps=lambda o: json.dumps(o, default=str))

    return handler


def create_app(service) -> web.Application:
    app = web.Application()
    app.add_routes([web.post(p, _dispatch(e)) for p, e in FullNodeRpcApi(service).get_routes().items()])
    return app
