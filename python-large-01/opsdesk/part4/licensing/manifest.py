"""Verification of signed subscription manifests.

Call through ``opsdesk.part4.licensing.upload.install_manifest``, which
bounds the upload before it reaches this module.
"""

import base64
import io
import json
import zipfile

from cryptography import x509
from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from django.utils.translation import gettext_lazy as _


def validate_entitlement_manifest(data):
    buff = io.BytesIO()
    buff.write(base64.b64decode(data))
    try:
        z = zipfile.ZipFile(buff)
    except zipfile.BadZipFile as e:
        raise ValueError(_("Invalid manifest: a subscription manifest zip file is required.")) from e
    buff = io.BytesIO()

    files = z.namelist()
    if 'consumer_export.zip' not in files or 'signature' not in files:
        raise ValueError(_("Invalid manifest: missing required files."))
    export = z.open('consumer_export.zip').read()
    sig = z.open('signature').read()
    with open('/etc/tower/candlepin-redhat-ca.crt', 'rb') as f:
        cert = x509.load_pem_x509_certificate(f.read(), backend=default_backend())
        key = cert.public_key()
    try:
        key.verify(sig, export, padding=padding.PKCS1v15(), algorithm=hashes.SHA256())
    except InvalidSignature as e:
        raise ValueError(_("Invalid manifest: signature verification failed.")) from e

    buff.write(export)
    z = zipfile.ZipFile(buff)
    subs = []
    for f in z.filelist:
        if f.filename.startswith('export/entitlements') and f.filename.endswith('.json'):
            subs.append(json.loads(z.open(f).read()))
    if subs:
        return subs
    raise ValueError(_("Invalid manifest: manifest contains no subscriptions."))
