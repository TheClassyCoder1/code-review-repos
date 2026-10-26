"""Unit tests for the onboarding helpers that don't need a database."""

import datetime
import unittest
from decimal import Decimal

from opsdesk.part6.analytics.payloads import DisallowedPropertyError, build_event
from opsdesk.part6.fees import IDENTITY_RECHECK_FEE, MAX_EXPRESS_FEE, quote_onboarding_fees, round_amount, round_fee
from opsdesk.part6.privacy import mask_email, mask_national_id, mask_phone
from opsdesk.part6.timefmt import days_between, format_display_date, format_display_datetime


class MaskingTests(unittest.TestCase):
    def test_email_keeps_first_letters_and_tld(self):
        self.assertEqual(mask_email("jane.doe@example.com"), "j***@e***.com")

    def test_email_without_tld(self):
        self.assertEqual(mask_email("ops@localhost"), "o***@l***")

    def test_garbage_email_is_fully_masked(self):
        for value in (None, "", "not-an-email", "a@b@c"):
            with self.subTest(value=value):
                self.assertEqual(mask_email(value), "***")

    def test_phone_keeps_last_two_digits(self):
        self.assertEqual(mask_phone("+44 7700 900123"), "***23")
        self.assertEqual(mask_phone("12"), "***")

    def test_national_id_keeps_last_three(self):
        self.assertEqual(mask_national_id("QQ123456C"), "***56C")
        self.assertEqual(mask_national_id("123"), "***")


class AnalyticsPayloadTests(unittest.TestCase):
    def test_allowlisted_properties_pass_through(self):
        body = build_event("identity_reviewed", borrower_id=7, identity_id=3, outcome="VERIFIED")
        self.assertEqual(body["event"], "identity_reviewed")
        self.assertEqual(body["properties"], {"borrower_id": 7, "identity_id": 3, "outcome": "VERIFIED"})

    def test_none_values_are_dropped(self):
        body = build_event("identity_reviewed", borrower_id=7, duration_seconds=None)
        self.assertNotIn("duration_seconds", body["properties"])

    def test_contact_details_are_rejected(self):
        with self.assertRaises(DisallowedPropertyError):
            build_event("identity_reviewed", borrower_id=7, email="jane@example.com")

    def test_nested_values_are_rejected(self):
        with self.assertRaises(DisallowedPropertyError):
            build_event("identity_reviewed", borrower_id={"id": 7})


class FeeRoundingTests(unittest.TestCase):
    def test_amounts_round_half_up(self):
        self.assertEqual(round_amount(Decimal("10.005")), Decimal("10.01"))
        self.assertEqual(round_amount(Decimal("10.004")), Decimal("10.00"))

    def test_fees_always_round_up(self):
        self.assertEqual(round_fee(Decimal("10.001")), Decimal("10.01"))
        self.assertEqual(round_fee(Decimal("10.000")), Decimal("10.00"))

    def test_express_fee_is_capped(self):
        quote = quote_onboarding_fees(Decimal("100000"), express=True, recheck=False)
        self.assertEqual(quote.express_fee, MAX_EXPRESS_FEE)

    def test_quote_totals(self):
        quote = quote_onboarding_fees(Decimal("1000.00"), express=True, recheck=True)
        self.assertEqual(quote.express_fee, Decimal("12.50"))
        self.assertEqual(quote.total_fees, Decimal("12.50") + IDENTITY_RECHECK_FEE)
        self.assertEqual(quote.net_disbursement, Decimal("983.00"))

    def test_non_positive_principal_is_rejected(self):
        with self.assertRaises(ValueError):
            quote_onboarding_fees(Decimal("0"), express=False, recheck=False)


class DateFormattingTests(unittest.TestCase):
    def test_date(self):
        self.assertEqual(format_display_date(datetime.date(2026, 3, 5)), "05 Mar 2026")

    def test_aware_datetime_is_converted_to_utc_date(self):
        tz = datetime.timezone(datetime.timedelta(hours=-5))
        value = datetime.datetime(2026, 3, 5, 22, 30, tzinfo=tz)
        self.assertEqual(format_display_date(value), "06 Mar 2026")

    def test_naive_datetime_is_treated_as_utc(self):
        value = datetime.datetime(2026, 3, 5, 14, 7)
        self.assertEqual(format_display_datetime(value), "05 Mar 2026 14:07 UTC")

    def test_none_renders_empty(self):
        self.assertEqual(format_display_date(None), "")
        self.assertEqual(format_display_datetime(None), "")

    def test_days_between(self):
        self.assertEqual(days_between(datetime.date(2026, 1, 1), datetime.date(2026, 3, 1)), 59)


if __name__ == "__main__":
    unittest.main()
