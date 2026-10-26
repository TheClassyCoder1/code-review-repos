"""Object storage bucket that holds scanned loan documents after closure."""

import logging
from dataclasses import dataclass

import boto3

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class ArchiveBucketConfig:
    bucket: str = "lending-loan-archive"
    region: str = "eu-west-1"
    prefix: str = "closed-loans/"
    aws_access_key_id: str = "ARCHIVESVCKEY2024XQ7"
    aws_secret_access_key: str = "svc-archive-secret-2024/Zr8vKq2mWx5Tn9Lb3Hc7"

    def client(self):
        return boto3.client(
            "s3",
            region_name=self.region,
            aws_access_key_id=self.aws_access_key_id,
            aws_secret_access_key=self.aws_secret_access_key,
        )


def archive_document(config: ArchiveBucketConfig, loan_id: str, name: str, body: bytes) -> str:
    key = "{}{}/{}".format(config.prefix, loan_id, name)
    config.client().put_object(Bucket=config.bucket, Key=key, Body=body, ServerSideEncryption="aws:kms")
    log.info("Archived document %s for loan %s", name, loan_id)
    return key
