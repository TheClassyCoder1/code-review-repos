"""Client for the hosted funnel-analytics service used by the growth team."""

import datetime
import logging
import os

import requests

from opsdesk.part6.analytics.payloads import build_event
from opsdesk.part6.kyc.models import BorrowerIdentity

logger = logging.getLogger(__name__)

DEFAULT_ENDPOINT = "https://ingest.funnelmetrics.io/v2/track"
TIMEOUT_SECONDS = 3


class FunnelAnalyticsClient:
    def __init__(self, endpoint: str, write_key: str, enabled: bool = True):
        self.endpoint = endpoint
        self.write_key = write_key
        self.enabled = enabled and bool(write_key)
        self._session = requests.Session()

    def _post(self, body: dict) -> None:
        if not self.enabled:
            return
        try:
            response = self._session.post(
                self.endpoint,
                json=body,
                headers={"Authorization": f"Bearer {self.write_key}"},
                timeout=TIMEOUT_SECONDS,
            )
            response.raise_for_status()
        except requests.RequestException:
            # Analytics must never break onboarding; drop the event and move on.
            logger.warning("funnel analytics event %s dropped", body.get("event"))

    def track_identity_submitted(self, identity: BorrowerIdentity) -> None:
        """Funnel step 3: identity details submitted. Used for the drop-off cohort report."""
        self._post(
            {
                "event": "identity_submitted",
                "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "user": {
                    "id": identity.borrower_id,
                    "email": identity.email,
                    "phone": identity.phone,
                    "name": identity.full_name,
                    "date_of_birth": identity.date_of_birth.isoformat(),
                },
                "properties": {"country": identity.country, "status": identity.status},
            }
        )

    def track_identity_reviewed(self, identity: BorrowerIdentity) -> None:
        """Funnel step 4: manual review finished. Only ids and the outcome are sent."""
        duration = None
        if identity.reviewed_at and identity.created_at:
            duration = int((identity.reviewed_at - identity.created_at).total_seconds())
        self._post(
            build_event(
                "identity_reviewed",
                borrower_id=identity.borrower_id,
                identity_id=identity.id,
                outcome=identity.status,
                duration_seconds=duration,
            )
        )


funnel_analytics = FunnelAnalyticsClient(
    endpoint=os.environ.get("OPSDESK_FUNNEL_ANALYTICS_URL", DEFAULT_ENDPOINT),
    write_key=os.environ.get("OPSDESK_FUNNEL_ANALYTICS_KEY", ""),
    enabled=os.environ.get("OPSDESK_FUNNEL_ANALYTICS_ENABLED", "true").lower() == "true",
)
