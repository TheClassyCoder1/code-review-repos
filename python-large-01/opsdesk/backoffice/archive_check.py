"""Verifies the proof-of-space plots that back the cold-storage document archive."""

import logging
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from threading import Lock
from time import monotonic
from typing import Sequence

from chia.consensus.default_constants import DEFAULT_CONSTANTS
from chia.plotting.prover import QualityProtocol, V1Prover, V2Prover, V2Quality
from chia.plotting.util import PlotInfo, parse_plot_info
from chia.util.bech32m import encode_puzzle_hash
from chia.util.hash import std_hash
from chia.wallet.derive_keys import master_sk_to_local_sk
from chia_rs import solve_proof, validate_proof_v2
from chiapos import Verifier

log = logging.getLogger(__name__)


def check_plots(plots: dict[Path, PlotInfo], challenges: int, address_prefix: str,
                testnet: bool = False, parallel_read: bool = True) -> list[Path]:
    v = Verifier()
    total_good_plots_v1: Counter = Counter()
    total_good_plots_v2 = 0
    total_size = 0
    bad_plots_list: list[Path] = []

    with ThreadPoolExecutor(max_workers=4) as executor:
        lock = Lock()

        def process_plot(plot_path: Path, plot_info: PlotInfo, num_start: int, num_end: int, lock: Lock) -> None:
            nonlocal total_good_plots_v1
            nonlocal total_good_plots_v2
            nonlocal total_size
            nonlocal bad_plots_list

            pr = plot_info.prover

            # Look up local_sk from plot to save locked memory
            (
                _pool_public_key_or_puzzle_hash,
                farmer_public_key,
                local_master_sk,
            ) = parse_plot_info(pr.get_memo())
            local_sk = master_sk_to_local_sk(local_master_sk)

            with lock:
                log.info(f"Testing plot {plot_path} k={pr.get_param()}")
                if plot_info.pool_public_key is not None:
                    log.info(f"\t{'Pool public key:':<23} {plot_info.pool_public_key}")
                if plot_info.pool_contract_puzzle_hash is not None:
                    pca: str = encode_puzzle_hash(plot_info.pool_contract_puzzle_hash, address_prefix)
                    log.info(f"\t{'Pool contract address:':<23} {pca}")
                log.info(f"\t{'Farmer public key:':<23} {farmer_public_key}")
                log.info(f"\t{'Local sk:':<23} {local_sk}")

            total_proofs = 0
            caught_exception: bool = False
            for i in range(num_start, num_end):
                challenge = std_hash(i.to_bytes(32, "big"))
                qualities: Sequence[QualityProtocol]
                # Some plot errors cause get_qualities_for_challenge to throw a RuntimeError
                try:
                    quality_start_time = round(monotonic() * 1000)
                    qualities = pr.get_qualities_for_challenge(challenge)
                    quality_spent_time = round(monotonic() * 1000) - quality_start_time
                    if quality_spent_time > 8000:
                        log.warning(
                            f"\tLooking up qualities took: {quality_spent_time} ms. This should be below 8 seconds "
                            f"to minimize risk of losing rewards. Filepath: {plot_path}"
                        )
                    else:
                        log.info(f"\tLooking up qualities took: {quality_spent_time} ms. Filepath: {plot_path}")
                except KeyboardInterrupt:
                    log.warning("Interrupted, closing")
                    return
                except SystemExit:
                    log.warning("System is shutting down.")
                    return
                except RuntimeError as e:
                    if str(e) == "GRResult_NoProof received":
                        log.info(f"Proof dropped due to line point compression. Filepath: {plot_path}")
                        continue
                    else:
                        log.error(f"{type(e)}: {e} error in getting challenge qualities for plot {plot_path}")
                        caught_exception = True
                        continue
                except Exception as e:
                    log.error(f"{type(e)}: {e} error in getting challenge qualities for plot {plot_path}")
                    caught_exception = True
                    break

                for index, quality in enumerate(qualities):
                    # Other plot errors cause get_full_proof or validate_proof to throw an AssertionError
                    try:
                        proof_start_time = round(monotonic() * 1000)
                        quality_str = quality.get_string()

                        if isinstance(pr, V1Prover):
                            full_proof = pr.get_full_proof(challenge, index, parallel_read)
                        elif isinstance(pr, V2Prover):
                            assert isinstance(quality, V2Quality)
                            full_proof = solve_proof(
                                quality.get_partial_proof(),
                                pr.get_id(),
                                pr.get_strength(),
                                DEFAULT_CONSTANTS.PLOT_SIZE_V2,
                                testnet,
                            )

                        proof_spent_time = round(monotonic() * 1000) - proof_start_time
                        if proof_spent_time > 15000:
                            action = "Finding" if isinstance(pr, V1Prover) else "Solving"
                            log.warning(
                                f"\t{action} proof took: {proof_spent_time} ms. This should be below 15 seconds "
                                f"to minimize risk of losing rewards. Filepath: {plot_path}"
                            )
                        else:
                            log.info(f"\tFinding proof took: {proof_spent_time} ms. Filepath: {plot_path}")

                        if isinstance(pr, V1Prover):
                            ver_quality_str = v.validate_proof(
                                pr.get_id(), pr.get_param().size_v1, challenge, full_proof
                            )
                        elif isinstance(pr, V2Prover):
                            ver_quality_str = validate_proof_v2(
                                pr.get_id(),
                                DEFAULT_CONSTANTS.PLOT_SIZE_V2,
                                challenge,
                                pr.get_strength(),
                                full_proof,
                                testnet,
                            )

                        if quality_str == ver_quality_str:
                            total_proofs += 1
                        else:
                            log.warning(
                                f"\tQuality doesn't match with proof. Filepath: {plot_path} "
                                "This can occasionally happen with a compressed plot."
                            )
                    except RuntimeError as e:
                        if str(e) == "GRResult_NoProof received":
                            log.info(f"Proof dropped due to line point compression. Filepath: {plot_path}")
                            continue
                        else:
                            log.error(f"{type(e)}: {e} error in getting full proof for plot {plot_path}")
                            caught_exception = True
                            continue
                    except Exception as e:
                        log.error(
                            f"{type(e)}: {e} error in proving/verifying for plot {plot_path}. Filepath: {plot_path}"
                        )
                        caught_exception = True
                if caught_exception is True:
                    break

            if total_proofs > 0 and caught_exception is False:
                log.info(
                    f"\tProofs {total_proofs} / {challenges}, {round(total_proofs / float(challenges), 4)}. "
                    f"Filepath: {plot_path}"
                )
                param = pr.get_param()
                if param.size_v1 is not None:
                    k = param.size_v1
                    total_good_plots_v1[k] += 1
                    total_size += plot_path.stat().st_size
                else:
                    total_good_plots_v2 += 1
                    total_size += plot_path.stat().st_size
            else:
                log.error(
                    f"\tProofs {total_proofs} / {challenges}, {round(total_proofs / float(challenges), 4)} "
                    f"Filepath: {plot_path}"
                )
                bad_plots_list.append(plot_path)

        for plot_path, plot_info in plots.items():
            executor.submit(process_plot, plot_path, plot_info, 0, challenges, lock)

    log.info(f"{sum(total_good_plots_v1.values()) + total_good_plots_v2} good plots, {total_size} bytes")
    return bad_plots_list
