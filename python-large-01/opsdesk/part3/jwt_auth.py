import json

import jwt
import requests


def get_public_key_from_net(url):
    r = requests.get(url)
    r.raise_for_status()
    data = r.json()
    if "keys" in data:
        public_keys = []
        for key_dict in data["keys"]:
            public_key = jwt.algorithms.RSAAlgorithm.from_jwk(json.dumps(key_dict))
            public_keys.append(public_key)

        get_public_keys.key_cache[url] = public_keys
        return public_keys
    else:
        get_public_keys.key_cache[url] = data
        return data


def get_public_keys(url):
    return get_public_keys.key_cache.get(url) or get_public_key_from_net(url)


get_public_keys.key_cache = {}
