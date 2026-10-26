import json

import requests
from flask import current_app


def _generate_header():
    return {"Authorization": f"Bearer {current_app.config['ACME_ULTRADNS_TOKEN']}", "Content-Type": "application/json"}


def _post(path, params):
    """Executes a POST request on given URL. Body is sent in JSON format"""
    base_uri = current_app.config.get("ACME_ULTRADNS_DOMAIN", "")
    resp = requests.post(
        f"{base_uri}{path}",
        headers=_generate_header(),
        data=json.dumps(params),
        verify=True,
    )
    resp.raise_for_status()


def create_txt_record(domain, token, zone_name):
    _post(f"/v2/zones/{zone_name}/rrsets/TXT/{domain}.", {"ttl": 5, "rdata": [token]})
