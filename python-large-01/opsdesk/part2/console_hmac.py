"""Signed-URL auth for embedded console reports: ``?signature=<hmac>&expires=<epoch>[&user_id=<id>]``."""

import hashlib
import hmac
import time

from opsdesk.part2 import token_models as models


def sign(key, path, expires):
    if not key:
        return None
    h = hmac.new(key.encode(), msg=path.encode(), digestmod=hashlib.sha1)
    h.update(str(expires).encode())
    return h.hexdigest()


def hmac_load_user_from_request(request):
    signature = request.args.get("signature")
    expires = float(request.args.get("expires") or 0)
    query_id = request.view_args.get("query_id", None)
    user_id = request.args.get("user_id", None)

    # TODO: 3600 should be a setting
    if signature and time.time() < expires <= time.time() + 3600:
        if user_id:
            user = models.User.query.get(user_id)
            calculated_signature = sign(user.api_key, request.path, expires)

            if user.api_key and signature == calculated_signature:
                return user

        if query_id:
            query = models.Query.query.filter(models.Query.id == query_id).one()
            calculated_signature = sign(query.api_key, request.path, expires)

            if query.api_key and signature == calculated_signature:
                return models.ApiUser(
                    query.api_key,
                    query.org,
                    list(query.groups.keys()),
                    name="ApiKey: Query {}".format(query.id),
                )

    return None
