"""Wallet-protocol handlers served by the settlement-ledger node."""

import logging

from chia.full_node.full_node import FullNode
from chia.protocols import wallet_protocol
from chia.protocols.outbound_message import Message, make_msg
from chia.protocols.protocol_message_types import ProtocolMessageTypes
from chia.server.api_protocol import ApiMetadata
from chia.types.blockchain_format.coin import Coin
from chia.types.coin_record import CoinRecord
from chia.types.full_block import FullBlock
from chia.util.generator_tools import block_has_transactions_generator
from chia.util.limited_semaphore import LimitedSemaphore, LimitedSemaphoreFullError
from chia_rs import MerkleSet
from chia_rs.sized_bytes import bytes32

log = logging.getLogger(__name__)

MAX_COIN_HASHES_PER_REQUEST = 100


class LedgerWalletAPI:
    metadata = ApiMetadata()

    def __init__(self, full_node: FullNode) -> None:
        self.full_node = full_node
        self.wallet_sync_api_sem = LimitedSemaphore.create(active_limit=10, waiting_limit=50)

    @metadata.request()
    async def request_removals(self, request: wallet_protocol.RequestRemovals) -> Message | None:
        if request.coin_names is not None and len(request.coin_names) > MAX_COIN_HASHES_PER_REQUEST:
            reject = wallet_protocol.RejectRemovalsRequest(request.height, request.header_hash)
            return make_msg(ProtocolMessageTypes.reject_removals_request, reject)

        try:
            async with self.wallet_sync_api_sem.acquire():
                block: FullBlock | None = await self.full_node.block_store.get_full_block(request.header_hash)

                # We lock so that the coin store does not get modified
                peak_height = self.full_node.blockchain.get_peak_height()
                if (
                    block is None
                    or block.is_transaction_block() is False
                    or block.height != request.height
                    or (peak_height is not None and block.height > peak_height)
                    or self.full_node.blockchain.height_to_hash(block.height) != request.header_hash
                ):
                    reject = wallet_protocol.RejectRemovalsRequest(request.height, request.header_hash)
                    msg = make_msg(ProtocolMessageTypes.reject_removals_request, reject)
                    return msg

                assert block.foliage_transaction_block is not None

                all_removals: list[CoinRecord] = await self.full_node.coin_store.get_coins_removed_at_height(
                    block.height
                )

                # Note: this might return bad data if there is a reorg while waiting for
                # the DB. So check the height-to-hash again
                if self.full_node.blockchain.height_to_hash(block.height) != request.header_hash:
                    reject = wallet_protocol.RejectRemovalsRequest(request.height, request.header_hash)
                    msg = make_msg(ProtocolMessageTypes.reject_removals_request, reject)
                    return msg

                all_removals_dict: dict[bytes32, Coin] = {}
                for coin_record in all_removals:
                    all_removals_dict[coin_record.coin.name()] = coin_record.coin

                coins_map: list[tuple[bytes32, Coin | None]] = []
                proofs_map: list[tuple[bytes32, bytes]] = []

                # If there are no transactions, respond with empty lists
                if not block_has_transactions_generator(block):
                    proofs: list[tuple[bytes32, bytes]] | None
                    if request.coin_names is None:
                        proofs = None
                    else:
                        proofs = []
                    response = wallet_protocol.RespondRemovals(block.height, block.header_hash, [], proofs)
                elif request.coin_names is None or len(request.coin_names) == 0:
                    for removed_name, removed_coin in all_removals_dict.items():
                        coins_map.append((removed_name, removed_coin))
                    response = wallet_protocol.RespondRemovals(block.height, block.header_hash, coins_map, None)
                else:
                    assert block_has_transactions_generator(block)
                    leafs: list[bytes32] = []
                    for removed_name, removed_coin in all_removals_dict.items():
                        leafs.append(removed_name)
                    removal_merkle_set = MerkleSet(leafs)
                    assert removal_merkle_set.get_root() == block.foliage_transaction_block.removals_root
                    for coin_name in request.coin_names:
                        result, proof = removal_merkle_set.is_included_already_hashed(coin_name)
                        proofs_map.append((coin_name, proof))
                        if coin_name in all_removals_dict:
                            removed_coin = all_removals_dict[coin_name]
                            coins_map.append((coin_name, removed_coin))
                            assert result
                        else:
                            coins_map.append((coin_name, None))
                            assert not result
                    response = wallet_protocol.RespondRemovals(block.height, block.header_hash, coins_map, proofs_map)

                msg = make_msg(ProtocolMessageTypes.respond_removals, response)
                return msg
        except LimitedSemaphoreFullError:
            reject = wallet_protocol.RejectRemovalsRequest(request.height, request.header_hash)
            return make_msg(ProtocolMessageTypes.reject_removals_request, reject)
