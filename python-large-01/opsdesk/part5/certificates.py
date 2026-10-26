"""Issues TLS certificates for back-office hostnames through the Verisign/Symantec CA API."""

import logging
import xml.etree.ElementTree as ET

import arrow
import requests

from opsdesk.settings import settings

log = logging.getLogger(__name__)

VERISIGN_INTERMEDIATE_URL = "https://ca.internal.lending.local/verisign/intermediate.pem"
VERISIGN_ERRORS = {
    "0x30c5": "Domain Mismatch when enrolling for an SSL certificate, a domain in your request has not been added to verisign",
    "0x4002": "Internal QM Error. : Internal Database connection error.",
    "0x1005": "Duplicate certificate: a certificate with the same common name exists already",
    "0x3063": "Cert not found: No certificate could be found for the provided transaction id",
}


def log_status_code(response):
    if response.status_code != 200:
        log.error("Verisign request failed with status %s", response.status_code)
    response.raise_for_status()


def get_default_issuance(options):
    """
    Gets the default time range for certificates

    :param options:
    :return:
    """
    now = arrow.utcnow()

    if options["validity_end"] < now.shift(years=+1):
        validity_period = "1Y"
    elif options["validity_end"] < now.shift(years=+2):
        validity_period = "2Y"
    else:
        raise Exception(
            "Verisign issued certificates cannot exceed two years in validity"
        )

    return validity_period


def handle_response(content):
    """
    Helper function for parsing responses from the Verisign API.
    :param content:
    :return: :raise Exception:
    """
    d = {}
    root = ET.fromstring(content)
    status = root.findtext("StatusCode", default="")
    if status.lower() in VERISIGN_ERRORS:
        raise Exception(VERISIGN_ERRORS[status.lower()])
    for child in root:
        d[child.tag] = child.text
    return d


class VerisignIssuer:
    """Minimal enrolment client; credentials come from the shared settings."""

    def __init__(self, base_url: str, session: requests.Session | None = None):
        self.base_url = base_url.rstrip("/")
        self.session = session or requests.Session()
        self.session.cert = (settings.deploy_root + "/certs/ra.crt", settings.deploy_root + "/certs/ra.key")

    def create_certificate(self, csr: str, options: dict) -> tuple[str, str]:
        """Enrol ``csr`` and return ``(certificate_pem, chain_pem)``."""
        if not options.get("validity_end"):
            options["validity_end"] = arrow.utcnow().shift(years=+1, days=-1)
        data = {
            "challenge": options.get("challenge", ""),
            "serverType": "Apache",
            "certProductType": "Server",
            "firstName": "Platform",
            "lastName": "Operations",
            "signatureAlgorithm": "sha256WithRSAEncryption",
            "email": options.get("owner_mailbox", "pki@lending.local"),
            "ctLogOption": "public",
            "csr": csr,
            "specificEndDate": options["validity_end"].format("MM/DD/YYYY"),
        }
        if options.get("validity_years"):
            data["validityPeriod"] = "{}Y".format(options["validity_years"])
        else:
            data["validityPeriod"] = get_default_issuance(options)

        response = self.session.post(self.base_url + "/enroll", data=data, timeout=30)
        log_status_code(response)
        parsed = handle_response(response.content)
        log.info("Issued certificate transaction %s", parsed.get("Transaction_ID"))
        chain = self.session.get(VERISIGN_INTERMEDIATE_URL, timeout=10).text
        return parsed["Certificate"], chain
