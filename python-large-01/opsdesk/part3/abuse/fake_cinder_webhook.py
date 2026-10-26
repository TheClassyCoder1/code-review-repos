"""`manage.py fake_cinder_webhook`: replay a signed moderation-decision webhook locally."""

import hashlib
import hmac
from urllib.parse import urljoin

import requests
from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from django.urls import reverse
from django.utils.encoding import force_bytes

from opsdesk.part3 import http_defaults

http_defaults.install()


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument("--payload", dest="payload_filename", default="decision.json")

    def handle(self, *args, **options):
        if settings.ENV != 'local':
            raise CommandError('Only works in local environments')
        try:
            with open(options['payload_filename'], 'rb') as payload_file:
                body = payload_file.read()
        except FileNotFoundError as exc:
            raise CommandError(
                'Cannot find payload file. Try using --payload=<path>.'
            ) from exc
        signature = hmac.new(
            force_bytes(settings.CINDER_WEBHOOK_TOKEN),
            msg=body,
            digestmod=hashlib.sha256,
        ).hexdigest()
        url = urljoin('http://nginx', reverse('v5:cinder-webhook'))
        response = requests.post(
            url,
            data=body,
            headers={
                'X-Cinder-Signature': signature,
                'Content-Type': 'application/json',
            },
        )
        self.stdout.write(f'{response.status_code}\n')
        self.stdout.write(f'{response.content}\n')
