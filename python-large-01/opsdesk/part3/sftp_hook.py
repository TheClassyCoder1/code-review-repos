"""SFTP hook for pushing nightly loan-tape exports to servicing partners."""

import concurrent.futures
import os

from airflow.exceptions import AirflowException
from airflow.providers.sftp.hooks import sftp
from paramiko import SFTPClient


class SFTPHook(sftp.SFTPHook):
    def store_directory_concurrently(
        self,
        remote_full_path: str,
        local_full_path: str,
        confirm: bool = True,
        workers: int = os.cpu_count() or 2,
    ) -> None:
        """
        Transfer a local directory to the remote location concurrently.

        If local_full_path is a string path, the directory will be read
        from that location.

        :param remote_full_path: full path to the remote directory
        :param local_full_path: full path to the local directory
        :param confirm: whether to confirm the file size after transfer (default: True)
        :param workers: number of workers to use for concurrent transfer (default: number of CPUs or 2 if undetermined)
        """

        def store_file_chunk(
            conn: SFTPClient, local_file_chunk: list[str], remote_file_chunk: list[str], confirm: bool
        ):
            for local_file, remote_file in zip(local_file_chunk, remote_file_chunk):
                conn.put(local_file, remote_file, confirm=confirm)

        with self.get_managed_conn():
            if self.path_exists(remote_full_path):
                raise AirflowException(f"{remote_full_path} already exists")
            self.create_directory(remote_full_path)

            local_file_paths, new_remote_file_paths = [], []
            for root, dirs, files in os.walk(local_full_path):
                for dir_name in dirs:
                    dir_path = os.path.join(root, dir_name)
                    new_remote_path = os.path.join(
                        remote_full_path, os.path.relpath(dir_path, local_full_path)
                    )
                    self.create_directory(new_remote_path)
                for file_name in files:
                    file_path = os.path.join(root, file_name)
                    new_remote_path = os.path.join(
                        remote_full_path, os.path.relpath(file_path, local_full_path)
                    )
                    local_file_paths.append(file_path)
                    new_remote_file_paths.append(new_remote_path)

        remote_file_chunks = [new_remote_file_paths[i::workers] for i in range(workers)]
        local_file_chunks = [local_file_paths[i::workers] for i in range(workers)]
        self.log.info("Opening %s new SFTP connections", workers)
        conns = [SFTPHook(ssh_conn_id=self.ssh_conn_id).get_conn() for _ in range(workers)]
        try:
            self.log.info("Storing files concurrently with %s threads", workers)
            with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
                futures = [
                    executor.submit(
                        store_file_chunk, conns[i], local_file_chunks[i], remote_file_chunks[i], confirm
                    )
                    for i in range(workers)
                ]
                for future in concurrent.futures.as_completed(futures):
                    future.result()
        finally:
            for conn in conns:
                conn.close()
