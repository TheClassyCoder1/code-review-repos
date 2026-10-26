"""URL signing from many import threads. `GCS._get_default_credentials` checks,
refreshes and replaces a shared cache in steps, so all calls hold `_CREDENTIALS_LOCK`.
"""

import datetime
import threading

from google.cloud import storage

from opsdesk.part3.storages.gcs_utils import GCS

_CREDENTIALS_LOCK = threading.Lock()


def generate_download_signed_url(bucket_name: str, blob_name: str) -> str:
    with _CREDENTIALS_LOCK:
        creds = dict(GCS._get_default_credentials() or {})
    return storage.Client().bucket(bucket_name).blob(blob_name).generate_signed_url(
        version="v4", expiration=datetime.timedelta(minutes=15),
        service_account_email=creds.get("service_account_email"), access_token=creds.get("access_token"))
