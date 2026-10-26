from typing import Sequence

import sqlalchemy as db
from dagster._core.storage.event_log import sql_event_log
from dagster._core.storage.event_log.schema import DynamicPartitionsTable
from dagster._core.storage.sqlalchemy_compat import db_select


class SqlEventLogStorage(sql_event_log.SqlEventLogStorage):
    def add_dynamic_partitions(
        self, partitions_def_name: str, partition_keys: Sequence[str]
    ) -> None:
        self._check_partitions_table()
        with self.index_connection() as conn:
            existing_rows = conn.execute(
                db_select([DynamicPartitionsTable.c.partition]).where(
                    db.and_(
                        DynamicPartitionsTable.c.partition.in_(partition_keys),
                        DynamicPartitionsTable.c.partitions_def_name == partitions_def_name,
                    )
                )
            ).fetchall()
            existing_keys = set([row[0] for row in existing_rows])
            new_keys = [
                partition_key
                for partition_key in partition_keys
                if partition_key not in existing_keys
            ]

            if new_keys:
                conn.execute(
                    DynamicPartitionsTable.insert(),
                    [
                        dict(partitions_def_name=partitions_def_name, partition=partition_key)
                        for partition_key in new_keys
                    ],
                )
