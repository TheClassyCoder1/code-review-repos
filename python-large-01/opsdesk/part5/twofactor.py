"""Two-factor login for back-office staff: OTP app enrolment and email codes."""

import os
from base64 import b32encode
from urllib.parse import quote

import pyotp
from django.core import mail
from django.core.cache import cache
from django.utils.translation import gettext as _

from opsdesk.settings import settings

QRCODE_LINK_TTL = 10 * 60


def get_default(key):
	"""Per-user flags (e.g. whether the OTP app login was already set up)."""
	return cache.get("2fa-default:" + key)


def set_default(key, value):
	cache.set("2fa-default:" + key, value, timeout=None)


def get_link_for_qrcode(user, totp_uri, otp_secret):
	"""Store the provisioning URI behind a random, short-lived key and return its link."""
	key = b32encode(os.urandom(15)).decode("utf-8").lower()
	cache.set("2fa-qr:" + key, {"user": user, "uri": totp_uri}, timeout=QRCODE_LINK_TTL)
	return "{}/backoffice/qrcode?k={}".format(settings.site_url, quote(key))


def get_email_subject_for_qr_code(kwargs_dict):
	return _("One Time Password (OTP) Registration Code from the back-office")


def get_email_body_for_qr_code(kwargs_dict):
	return _(
		"Please click on the following link and follow the instructions on the page. {0}"
	).format(kwargs_dict["qrcode_link"])


def get_email_body_for_2fa(kwargs_dict):
	return _("Enter Code displayed in OTP App.") if not kwargs_dict.get("otp") else _(
		"Your verification code is {0}"
	).format(kwargs_dict["otp"])


def send_token_via_email(user, token, otp_secret, otp_issuer, subject=None, message=None):
	"""Send token to user as email."""
	hotp = pyotp.HOTP(otp_secret)
	otp = hotp.at(int(token))
	template_args = {"otp": otp, "otp_issuer": otp_issuer}
	if not subject:
		subject = _("Verification Code from {0}").format(otp_issuer)
	if not message:
		message = get_email_body_for_2fa(template_args)
	sent = mail.send_mail(subject, message, None, [user], fail_silently=True)
	return bool(sent)


def process_2fa_for_email(user, token, otp_secret, otp_issuer, method="Email"):
	"""Process Email method for 2fa."""
	subject = None
	message = None
	status = True
	prompt = ""
	if method == "OTP App" and not get_default(user + "_otplogin"):
		"""Sending one-time email for OTP App"""
		totp_uri = pyotp.TOTP(otp_secret).provisioning_uri(user, issuer_name=otp_issuer)
		qrcode_link = get_link_for_qrcode(user, totp_uri, otp_secret)
		message = get_email_body_for_qr_code({"qrcode_link": qrcode_link})
		subject = get_email_subject_for_qr_code({"qrcode_link": qrcode_link})
		prompt = _(
			"Please check your registered email address for instructions on how to proceed. Do not close this window as you will have to return to it."
		)
	else:
		"""Sending email verification"""
		prompt = _("Verification code has been sent to your registered email address.")
	status = send_token_via_email(user, token, otp_secret, otp_issuer, subject=subject, message=message)
	return {
		"token_delivery": status,
		"prompt": status and prompt,
		"method": "Email",
		"setup": status,
	}
