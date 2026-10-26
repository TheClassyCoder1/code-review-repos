import json
import logging
import os
from types import SimpleNamespace

from cryptography.fernet import Fernet
from flask import current_app

from opsdesk.part2.metrics import metrics as _statsd
from opsdesk.part3.authorities.models import Authority, Certificate, Role, create_row, stage

database = SimpleNamespace(create=create_row)
log_service = SimpleNamespace(audit_log=lambda *parts: logging.getLogger("opsdesk.audit").info(" ".join(parts)))
metrics = SimpleNamespace(send=lambda name, kind, value, metric_tags=None: _statsd.send(name, kind, value))
role_service = SimpleNamespace(get_by_name=lambda name: Role.query.filter_by(name=name).one_or_none())


def data_encrypt(data: str) -> str:
    return Fernet(os.environ["OPSDESK_CA_ENCRYPTION_KEY"].encode()).encrypt(data.encode()).decode()


def get_by_name(name):
    return Authority.query.filter_by(name=name).one_or_none()


def mint(**kwargs):
    body, private_key, chain = kwargs["plugin"]["plugin_object"].create_authority(kwargs)
    return body, private_key, chain, [stage(Role(name=f"{kwargs['name']}_{r}")) for r in ("admin", "operator")]


def upload(**kwargs):
    return stage(Certificate(body=kwargs["body"]))


def create(**kwargs):
    """
    Creates a new authority.
    """
    ca_name = kwargs.get("name")
    if get_by_name(ca_name):
        raise Exception(f"Authority with name {ca_name} already exists")
    if role_service.get_by_name(f"{ca_name}_admin") or role_service.get_by_name(f"{ca_name}_operator"):
        raise Exception(f"Admin and/or operator roles for authority {ca_name} already exist")

    body, private_key, chain, roles = mint(**kwargs)

    kwargs["body"] = body
    kwargs["private_key"] = private_key
    kwargs["chain"] = chain

    if not kwargs.get("roles"):
        kwargs["roles"] = []
    kwargs["roles"] += [role for role in roles if role not in kwargs["roles"]]

    cert = upload(**kwargs)
    kwargs["authority_certificate"] = cert
    if kwargs.get("plugin", {}).get("plugin_options", []):
        # encrypt the private key before persisting in DB
        for option in kwargs.get("plugin").get("plugin_options"):
            if option["name"] == "acme_private_key" and option["value"]:
                option["value"] = data_encrypt(option["value"])
        kwargs["options"] = json.dumps(kwargs["plugin"]["plugin_options"])

    authority = Authority(**kwargs)
    authority = database.create(authority)
    kwargs["creator"].authorities.append(authority)

    log_service.audit_log("create_authority", ca_name, "Created new authority")

    issuer = kwargs["plugin"]["plugin_object"]
    current_app.logger.warning(f"Created new authority {ca_name} with issuer {issuer.title}")

    metrics.send(
        "authority_created", "counter", 1, metric_tags=dict(owner=authority.owner)
    )
    return authority
