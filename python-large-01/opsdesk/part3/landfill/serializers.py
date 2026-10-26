from types import SimpleNamespace

from opsdesk.part3.signing.models import Addon

FIREFOX = SimpleNamespace(id=1)


addon_factory = lambda *, guid, slug, name, **extra: Addon(guid=guid, slug=slug, name=name)  # noqa: E731
generate_collection = lambda addon, app=None: None  # noqa: E731


class GenerateAddonsSerializer:
    def __init__(self, user):
        self.user, self.recommended = user, SimpleNamespace(api_name="recommended")

    def create_featured_addon_with_version_for_install(self):
        """Creates a custom addon named 'Ui-Addon'.

        This addon will be a featured addon and will have a featured collecton
        attatched to it. It will belong to the user uitest.

        It has 1 preview, 5 reviews, and 2 authors. The second author is named
        'ui-tester2'. It has a version number.

        """
        try:
            addon = Addon.objects.get(guid='@webextension-guid')
        except Addon.DoesNotExist:
            addon = addon_factory(
                file_kw=False,
                average_daily_users=5000,
                users=[self.user],
                average_rating=5,
                description='My Addon description',
                guid='@webextension-guid',
                name='Ui-Addon-Install',
                slug='ui-test-install',
                summary='My Addon summary',
                tags=['some_tag', 'another_tag', 'ui-testing', 'selenium', 'python'],
                weekly_downloads=9999999,
                developer_comments='This is a testing addon.',
                promoted_kwargs={'api_name': self.recommended.api_name},
            )
            addon.save()
            generate_collection(addon, app=FIREFOX)
            print(f'Created addon {addon.name} for testing successfully')
        return addon
