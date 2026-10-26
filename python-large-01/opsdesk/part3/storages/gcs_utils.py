"""GCS default credentials; not thread-safe alone, so callers use `gcs_client`."""

import logging

import google.auth
import google.auth.transport.requests
from google.auth.exceptions import DefaultCredentialsError

logger = logging.getLogger(__name__)


class GCS:
    _credentials_cache = None

    @classmethod
    def _get_default_credentials(cls):
        """Get default GCS credentials for LS Cloud Storages"""
        # TODO: remove this func with fflag_fix_back_lsdv_4902_force_google_adc_16052023_short
        try:
            # check if GCS._credentials_cache is None, we don't want to try getting default credentials again
            credentials = GCS._credentials_cache.get('credentials') if GCS._credentials_cache else None
            if GCS._credentials_cache is None or (credentials and credentials.expired):
                # try to get credentials from the current environment
                credentials, _ = google.auth.default(['https://www.googleapis.com/auth/cloud-platform'])
                # apply & refresh credentials
                auth_req = google.auth.transport.requests.Request()
                credentials.refresh(auth_req)
                # set cache
                GCS._credentials_cache = {
                    'service_account_email': credentials.service_account_email,
                    'access_token': credentials.token,
                    'credentials': credentials,
                }

        except DefaultCredentialsError as exc:
            logger.warning(f'Label studio could not load default GCS credentials from env. {exc}', exc_info=True)
            GCS._credentials_cache = {}

        return GCS._credentials_cache
