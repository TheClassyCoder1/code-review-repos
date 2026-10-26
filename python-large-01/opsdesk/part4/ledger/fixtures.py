"""Cached test chains for the settlement-ledger integration tests."""

import pickle
from pathlib import Path

from chia.simulator.block_tools import BlockTools
from chia.types.full_block import FullBlock
from chia_rs.sized_ints import uint64


def new_test_db(
    path: Path,
    num_of_blocks: int,
    seed: bytes,
    empty_sub_slots: int,
    bt: BlockTools,
    block_list_input: list[FullBlock],
    time_per_block: float | None,
    *,
    normalized_to_identity_cc_eos: bool = False,  # CC_EOS,
    normalized_to_identity_icc_eos: bool = False,  # ICC_EOS
    normalized_to_identity_cc_sp: bool = False,  # CC_SP,
    normalized_to_identity_cc_ip: bool = False,  # CC_IP
    dummy_block_references: bool = False,
    include_transactions: bool = False,
) -> list[FullBlock]:
    print(f"create {path} with {num_of_blocks} blocks with ")
    blocks: list[FullBlock] = bt.get_consecutive_blocks(
        num_of_blocks,
        block_list_input=block_list_input,
        time_per_block=time_per_block,
        seed=seed,
        skip_slots=empty_sub_slots,
        normalized_to_identity_cc_eos=normalized_to_identity_cc_eos,
        normalized_to_identity_icc_eos=normalized_to_identity_icc_eos,
        normalized_to_identity_cc_sp=normalized_to_identity_cc_sp,
        normalized_to_identity_cc_ip=normalized_to_identity_cc_ip,
        dummy_block_references=dummy_block_references,
        include_transactions=include_transactions,
        genesis_timestamp=uint64(1234567890),
    )
    block_bytes_list: list[bytes] = []
    for block in blocks:
        block_bytes_list.append(bytes(block))
    bytes_fn = pickle.dumps(block_bytes_list)
    path.write_bytes(bytes_fn)
    return blocks
