<?php

namespace Tests\Unit\Part6;

use App\Part6\Analytics\AnalyticsPayloadFilter;
use App\Part6\Hardship\ReliefCalculator;
use App\Part6\Support\BorrowerDates;
use App\Part6\Support\HardshipAmount;
use App\Part6\Support\PiiMasker;
use DateTimeImmutable;
use DateTimeZone;
use PHPUnit\Framework\TestCase;

class HardshipSupportTest extends TestCase
{
    public function test_email_mask_keeps_only_first_characters_and_tld(): void
    {
        $this->assertSame('j***@e***.com', PiiMasker::email('jane.doe@example.com'));
        $this->assertSame('***', PiiMasker::email('@'));
    }

    public function test_phone_mask_keeps_last_four_digits(): void
    {
        $this->assertSame('*********4821', PiiMasker::phone('+49 151 1234 4821'));
        $this->assertSame('***', PiiMasker::phone('123'));
    }

    public function test_relief_percentage_rounds_down(): void
    {
        // 3333 * 25.5% = 849.915 cents; a credit must not round up to 850.
        $this->assertSame(849, ReliefCalculator::percentOf(3333, 2550));
        $this->assertSame(0, ReliefCalculator::percentOf(1, 9999));
    }

    public function test_relief_is_capped_with_fees_waived_first(): void
    {
        $relief = (new ReliefCalculator(10000))->reliefFor(8000, 9000, 10000, 5000);

        $this->assertSame(8000, $relief['fee_relief_cents']);
        $this->assertSame(2000, $relief['interest_relief_cents']);
        $this->assertSame(10000, $relief['total_relief_cents']);
        $this->assertTrue($relief['capped']);
    }

    public function test_analytics_filter_drops_contact_fields(): void
    {
        $clean = AnalyticsPayloadFilter::idsOnly([
            'event' => 'hardship.step',
            'borrower_id' => 42,
            'application_id' => 7,
            'step' => 'submitted',
            'email' => 'jane.doe@example.com',
            'phone' => '+4915112344821',
            'reason' => 'Lost my job at Acme in March',
        ]);

        $this->assertSame([
            'event' => 'hardship.step',
            'borrower_id' => 42,
            'application_id' => 7,
            'step' => 'submitted',
        ], $clean);
    }

    public function test_display_dates_use_borrower_timezone(): void
    {
        $utc = new DateTimeImmutable('2026-04-02 23:30:00', new DateTimeZone('UTC'));

        $this->assertSame('3 Apr 2026', BorrowerDates::forDisplay($utc, 'Asia/Kolkata'));
        $this->assertSame('2 Apr 2026', BorrowerDates::forDisplay($utc, 'America/New_York'));
        $this->assertSame('2026-04-02', BorrowerDates::iso($utc));
    }

    public function test_hardship_amount_delegates_to_money(): void
    {
        $amounts = new HardshipAmount('EUR');

        $this->assertSame(125050, $amounts->parse('1250.50'));
        $this->assertSame('1250.50 EUR', $amounts->format(125050));
    }
}
