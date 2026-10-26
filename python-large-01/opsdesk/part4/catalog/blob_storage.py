"""Azure Blob import storage for bulk document review tasks."""

import os

from azure.storage.blob import BlobServiceClient
from django.db import models


class StorageObject:
    """One task's worth of data read from a storage key (a file or a row of one)."""

    def __init__(self, key, task_data, row_index=None, row_group=None):
        self.key = key
        self.task_data = task_data
        self.row_index = row_index
        self.row_group = row_group

    @classmethod
    def bulk_create(
        cls, task_datas: list[dict], key, row_indexes: list[int] | None = None, row_groups: list[int] | None = None
    ) -> list['StorageObject']:
        if row_indexes is None:
            row_indexes = [None] * len(task_datas)
        if row_groups is None:
            row_groups = [None] * len(task_datas)
        return [
            cls(key=key, row_index=row_idx, row_group=row_group, task_data=task_data)
            for row_idx, row_group, task_data in zip(row_indexes, row_groups, task_datas)
        ]


class AzureBlobImportStorage(models.Model):
    title = models.CharField(max_length=255)
    container = models.TextField()
    prefix = models.TextField(blank=True)
    account_name = models.TextField(blank=True)
    account_key = models.TextField(blank=True)

    class Meta:
        app_label = "backoffice"

    def get_account_name(self):
        return str(self.account_name) if self.account_name else os.environ.get("AZURE_BLOB_ACCOUNT_NAME")

    def get_account_key(self):
        return str(self.account_key) if self.account_key else os.environ.get("AZURE_BLOB_ACCOUNT_KEY")

    def get_client_and_container(self):
        account_name = self.get_account_name()
        account_key = self.get_account_key()
        if not account_name or not account_key:
            raise ValueError(
                'Azure account name and key must be set using '
                'environment variables AZURE_BLOB_ACCOUNT_NAME and AZURE_BLOB_ACCOUNT_KEY '
                'or account_name and account_key fields.'
            )
        connection_string = (
            'DefaultEndpointsProtocol=https;AccountName='
            + account_name
            + ';AccountKey='
            + account_key
            + ';EndpointSuffix=core.windows.net'
        )
        client = BlobServiceClient.from_connection_string(conn_str=connection_string)
        container = client.get_container_client(str(self.container))
        return client, container
