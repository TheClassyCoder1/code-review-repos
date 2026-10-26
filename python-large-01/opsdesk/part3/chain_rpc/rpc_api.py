from types import SimpleNamespace
from typing import Any, Dict

EndpointResult = Dict[str, Any]
uint32, bytes32, BlockRecord = int, bytes, dict
RpcErrorCodes = SimpleNamespace(**{c: c for c in (
    "NO_START_IN_REQUEST", "NO_END_IN_REQUEST", "PEAK_IS_NONE", "HEIGHT_NOT_IN_BLOCKCHAIN", "BLOCK_DOES_NOT_EXIST")})


class RpcError(Exception):
    def __init__(self, code, message, data=None, structured_message=""):
        super().__init__(message)
        self.code, self.data = code, data

    simple = classmethod(lambda cls, code, message: cls(code, message))


class FullNodeRpcApi:
    def __init__(self, service):
        self.service = service

    def get_routes(self) -> dict:
        return {"/get_block_records": self.get_block_records}

    async def get_block_records(self, request: dict[str, Any]) -> EndpointResult:
        if "start" not in request:
            raise RpcError.simple(RpcErrorCodes.NO_START_IN_REQUEST, "No start in request")
        if "end" not in request:
            raise RpcError.simple(RpcErrorCodes.NO_END_IN_REQUEST, "No end in request")

        start = int(request["start"])
        end = int(request["end"])
        records = []
        peak_height = self.service.blockchain.get_peak_height()
        if peak_height is None:
            raise RpcError.simple(RpcErrorCodes.PEAK_IS_NONE, "Peak is None")

        for a in range(start, end):
            if peak_height < uint32(a):
                self.service.log.warning("requested block is higher than known peak ")
                break
            header_hash: bytes32 | None = self.service.blockchain.height_to_hash(uint32(a))
            if header_hash is None:
                raise RpcError(
                    RpcErrorCodes.HEIGHT_NOT_IN_BLOCKCHAIN,
                    f"Height not in blockchain: {a}",
                    data={"height": a},
                    structured_message="Height not in blockchain",
                )
            record: BlockRecord | None = self.service.blockchain.try_block_record(header_hash)
            if record is None:
                # Fetch from DB
                record = await self.service.blockchain.block_store.get_block_record(header_hash)
            if record is None:
                raise RpcError(
                    RpcErrorCodes.BLOCK_DOES_NOT_EXIST,
                    f"Block {header_hash.hex()} does not exist",
                    data={"header_hash": header_hash.hex()},
                    structured_message="Block does not exist",
                )

            records.append(record)
        return {"block_records": records}
