"""Staff notification subscriptions (product listing updates, digests)."""

import logging

from django.conf import settings
from django.core import mail
from django.core.exceptions import ValidationError
from django.core.validators import EmailValidator
from django.db import models
from django.template import loader
from django.urls import reverse
from django.utils import timezone

user_log = logging.getLogger("opsdesk.backoffice.users")


class amo:
    CHANNEL_UNLISTED, CHANNEL_LISTED = 1, 2
    CHANNEL_CHOICES_API = {CHANNEL_UNLISTED: "unlisted", CHANNEL_LISTED: "listed"}


def absolutify(url):
    return settings.SITE_URL.rstrip("/") + url


def send_mail(subject, message, recipient_list, use_deny_list=True):
    deny = getattr(settings, "EMAIL_DENY_LIST", ()) if use_deny_list else ()
    return mail.send_mail(subject, message, None, [r for r in recipient_list if r not in deny])


class ProductListingSubscription(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    addon = models.ForeignKey("backoffice.CatalogueProduct", on_delete=models.CASCADE)

    def send_notification(self, version):
        user_log.info(
            'Sending addon update notice to %s for %s'
            % (self.user.email, self.addon.pk)
        )

        if version.channel == amo.CHANNEL_LISTED:
            listing_url = absolutify(
                reverse('addons.detail', args=[self.addon.pk], add_prefix=False)
            )
        else:
            # If the submission went to the unlisted or enterprise
            # channels, do not link to the listing.
            listing_url = None
        context = {
            'name': self.addon.name,
            'url': listing_url,
            'number': version.version,
            'channel': amo.CHANNEL_CHOICES_API[version.channel],
            'review': absolutify(
                reverse(
                    'reviewers.review',
                    kwargs={
                        'addon_id': self.addon.pk,
                        'channel': amo.CHANNEL_CHOICES_API[version.channel],
                    },
                    add_prefix=False,
                )
            ),
            'SITE_URL': settings.SITE_URL,
        }
        # Not being localised because we don't know the reviewer's locale.
        subject = 'Mozilla Add-ons: %s Updated' % self.addon.name
        template = loader.get_template('reviewers/emails/notify_update.ltxt')
        send_mail(
            subject,
            template.render(context),
            recipient_list=[self.user.email],
            use_deny_list=False,
        )


class Notification(models.Model):
    subscription = models.ForeignKey("backoffice.NotificationSubscription", on_delete=models.CASCADE)
    event_context = models.JSONField(null=True)
    sent = models.DateTimeField(null=True)
    fake_sent = models.BooleanField(default=False)

    def send(self, destination_address, email_context=None, save=True):
        name = self.subscription.notification_type.name
        mail.send_mail(name, loader.render_to_string(f"notifications/{name}.txt", self.event_context), None,
                       [destination_address])


class NotificationSubscription(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    notification_type = models.ForeignKey("backoffice.NotificationType", on_delete=models.CASCADE)
    message_frequency = models.CharField(max_length=16, default="instantly")

    @property
    def _is_digest(self):
        return self.message_frequency in ("daily", "weekly")

    def emit(
            self,
            event_context=None,
            destination_address=None,
            email_context=None,
            save=True,
    ):
        """Emit a notification to a user by creating Notification and NotificationSubscription objects.

        Args:
            event_context (OSFUser): The info for context for the template
            destination_address (optional): overides the user's email address for the notification. Good for sending
            to a test address or OSF desk support'
            email_context (dict, optional): Context for sending the email bcc, reply_to header etc
            save (bool, optional): save the notification and creates a subscription object if true, otherwise just
            send the notification with no db transaction, therefore message_frequency should always be 'instantly' when
            used.
        """
        if not settings.CI_ENV:
            logging.info(
                f"Attempting to create Notification:"
                f"\nto={getattr(self.user, 'username', destination_address)}"
                f"\ntype={self.notification_type.name}"
                f"\nmessage_frequency={self.message_frequency}"
                f"\nevent_context={event_context}"
                f"\nemail_context={email_context}"
            )

        if not destination_address:
            destination_address = self.user.email
            validator = EmailValidator()
            try:
                validator(destination_address)
            except ValidationError:
                emails_qs = self.user.emails
                if emails_qs.exists():
                    destination_address = emails_qs.first().address
                try:
                    validator(destination_address)
                except ValidationError:
                    return

        if self.message_frequency == 'instantly':
            notification = Notification(
                subscription=self,
                event_context=event_context
            )
            if save:
                notification.save()
            else:
                notification.send(
                    destination_address=destination_address,
                    email_context=email_context,
                    save=save,
                )
                return
            if not self._is_digest:
                notification.send(
                    destination_address=destination_address,
                    email_context=email_context,
                    save=save,
                )
        else:
            Notification.objects.create(
                subscription=self,
                event_context=event_context,
                sent=timezone.now() if self.message_frequency == 'none' else None,
                fake_sent=True if self.message_frequency == 'none' else False,
            )
