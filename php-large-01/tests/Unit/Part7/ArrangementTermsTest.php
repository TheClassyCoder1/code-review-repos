<?php

namespace Tests\Unit\Part7;

use App\Part7\Arrangements\AffordabilityAssessor;
use App\Part7\Arrangements\ArrangementTermsCalculator;
use DateTimeImmutable;
use InvalidArgumentException;
use PHPUnit\Framework\TestCase;

class ArrangementTermsTest extends TestCase
{
    private function calculator(): ArrangementTermsCalculator
    {
        return new ArrangementTermsCalculator('EUR', 2, 24, 2500, 30);
    }

    public function test_instalments_sum_back_to_arrears_with_rounding_cents_first(): void
    {
        $quote = $this->calculator()->quote(
            10000,
            3,
            new DateTimeImmutable('2026-11-10'),
            new DateTimeImmutable('2026-10-26'),
        );

        $amounts = array_column($quote['schedule'], 'amount_cents');
        $this->assertSame([3334, 3333, 3333], $amounts);
        $this->assertSame(10000, array_sum($amounts));
        $this->assertSame(3334, $quote['instalment_cents']);
    }

    public function test_due_dates_keep_day_of_month_and_clamp_to_month_end(): void
    {
        $quote = $this->calculator()->quote(
            90000,
            4,
            new DateTimeImmutable('2027-01-31'),
            new DateTimeImmutable('2027-01-05'),
        );

        $this->assertSame(
            ['2027-01-31', '2027-02-28', '2027-03-31', '2027-04-30'],
            array_column($quote['schedule'], 'due_on'),
        );
    }

    public function test_month_arithmetic_handles_leap_february(): void
    {
        $date = ArrangementTermsCalculator::monthsAfter(new DateTimeImmutable('2028-01-30'), 1);

        $this->assertSame('2028-02-29', $date->format('Y-m-d'));
    }

    public function test_rejects_first_due_date_today_or_too_far_out(): void
    {
        $today = new DateTimeImmutable('2026-10-26');

        foreach (['2026-10-26', '2026-11-26'] as $firstDue) {
            try {
                $this->calculator()->quote(50000, 4, new DateTimeImmutable($firstDue), $today);
                $this->fail("Expected {$firstDue} to be rejected.");
            } catch (InvalidArgumentException $e) {
                $this->assertStringContainsString('first instalment', $e->getMessage());
            }
        }
    }

    public function test_rejects_instalments_below_minimum_payment(): void
    {
        $this->expectException(InvalidArgumentException::class);

        $this->calculator()->quote(6000, 3, new DateTimeImmutable('2026-11-01'), new DateTimeImmutable('2026-10-26'));
    }

    public function test_rejects_count_outside_bounds(): void
    {
        $this->expectException(InvalidArgumentException::class);

        $this->calculator()->quote(500000, 25, new DateTimeImmutable('2026-11-01'), new DateTimeImmutable('2026-10-26'));
    }

    public function test_count_for_target_rounds_up_and_respects_bounds(): void
    {
        $calculator = $this->calculator();

        $this->assertSame(4, $calculator->countForTarget(10000, 3000));
        $this->assertSame(2, $calculator->countForTarget(1000, 5000));
        $this->assertNull($calculator->countForTarget(1000000, 1000));
        $this->assertNull($calculator->countForTarget(1000, 0));
    }

    public function test_income_share_rounds_up(): void
    {
        // 1001 / 2500 = 40.04% -> 4004 bps; 1000.1 / 2500 must not read as exactly 40%.
        $this->assertSame(4004, AffordabilityAssessor::shareBps(1001, 2500));
        $this->assertSame(4000, AffordabilityAssessor::shareBps(1000, 2500));
        $this->assertSame(4001, AffordabilityAssessor::shareBps(100001, 250000));
    }

    public function test_income_share_with_nothing_left_over_exceeds_ceiling(): void
    {
        $this->assertGreaterThan(10000, AffordabilityAssessor::shareBps(500, 0));
        $this->assertGreaterThan(10000, AffordabilityAssessor::shareBps(500, -2000));
    }
}
