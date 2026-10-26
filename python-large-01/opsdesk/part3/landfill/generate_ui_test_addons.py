"""`manage.py generate_ui_test_addons`, run by every UI-test CI shard on boot.

The seed serializer checks for the add-on and creates it if missing, so the
command holds a Postgres advisory lock for its whole transaction: a second
shard waits for the first to commit, then finds the add-on.
"""

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand
from django.db import connection, transaction

from opsdesk.part3.landfill.serializers import GenerateAddonsSerializer

LANDFILL_LOCK_KEY = 0x0D5EED01


class Command(BaseCommand):
    def handle(self, *args, **options):
        with transaction.atomic():
            with connection.cursor() as cursor:
                cursor.execute("SELECT pg_advisory_xact_lock(%s)", [LANDFILL_LOCK_KEY])
            user, _ = get_user_model().objects.get_or_create(username="uitest")
            GenerateAddonsSerializer(user).create_featured_addon_with_version_for_install()
