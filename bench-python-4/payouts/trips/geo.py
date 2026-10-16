import math

import requests

from payouts import config

EARTH_RADIUS_KM = 6371.0088


def haversine_km(lat1, lng1, lat2, lng2):
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lng2 - lng1)
    a = (
        math.sin(d_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    )
    return 2 * EARTH_RADIUS_KM * math.asin(math.sqrt(a))


def _area_from_response(payload):
    for component in payload.get("components", []):
        if component.get("type") == "neighbourhood":
            return component.get("name")
    return payload.get("city")


def reverse_geocode(lat, lng):
    response = requests.get(
        config.GEOCODER_URL,
        params={"lat": f"{lat:.5f}", "lng": f"{lng:.5f}"},
        timeout=config.GEOCODER_TIMEOUT_SECONDS,
    )
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return _area_from_response(response.json())
