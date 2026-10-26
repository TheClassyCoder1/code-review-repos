<?php

namespace Tests\Unit\Part4;

use App\Part4\Remittance\RemittanceLine;
use App\Part4\Remittance\RemittanceMatcher;
use App\Part4\Schedule\AmortizationCalculator;
use App\Part4\Support\BankAccountValidator;
use App\Part4\Support\Money;
use DateTimeImmutable;
use InvalidArgumentException;
use PHPUnit\Framework\TestCase;

class MoneyAndScheduleTest extends TestCase
{
    public function test_parses_decimal_strings_without_floats(): void
    {
        $this->assertSame(125050, Money::fromDecimalString('1250.50', 'inr')->minor);
        $this->assertSame(-310, Money::fromDecimalString('-3.1', 'EUR')->minor);
        $this->assertSame(1500, Money::fromDecimalString('1500', 'JPY')->minor);
    }

    public function test_rejects_excess_precision(): void
    {
        $this->expectException(InvalidArgumentException::class);

        Money::fromDecimalString('10.001', 'USD');
    }

    public function test_allocation_sums_back_to_total(): void
    {
        $parts = Money::ofMinor(1000, 'USD')->allocate([1, 1, 1]);

        $this->assertSame([334, 333, 333], array_map(fn (Money $m) => $m->minor, $parts));
    }

    public function test_formats_minor_units(): void
    {
        $this->assertSame('0.05', Money::ofMinor(5, 'EUR')->toDecimalString());
        $this->assertSame('-12.345', Money::ofMinor(-12345, 'KWD')->toDecimalString());
    }

    public function test_iban_checksum(): void
    {
        $validator = new BankAccountValidator();

        $this->assertTrue($validator->isValidIban('GB82 WEST 1234 5698 7654 32'));
        $this->assertFalse($validator->isValidIban('GB82 WEST 1234 5698 7654 33'));
        $this->assertSame('******7890', $validator->mask('1234567890'));
    }

    public function test_schedule_closes_at_zero_and_clamps_month_end(): void
    {
        $calculator = new AmortizationCalculator();
        $schedule = $calculator->equalInstallmentSchedule(1200000, 1200, 12, new DateTimeImmutable('2026-01-31'));

        $this->assertCount(12, $schedule);
        $this->assertSame(0, end($schedule)->closingBalanceMinor);
        $this->assertSame(1200000, array_sum(array_map(fn ($i) => $i->principalMinor, $schedule)));
        $this->assertSame('2026-02-28', $schedule[1]->dueOn->format('Y-m-d'));
    }

    public function test_matcher_pays_oldest_installment_first(): void
    {
        $line = new RemittanceLine('TXN-1', 'LN-1', Money::ofMinor(1500, 'INR'), new DateTimeImmutable('2026-10-01'));
        $open = [
            (object) ['id' => 10, 'principal_minor' => 800, 'interest_minor' => 200, 'paid_minor' => 400],
            (object) ['id' => 11, 'principal_minor' => 800, 'interest_minor' => 200, 'paid_minor' => 0],
        ];

        $plan = (new RemittanceMatcher())->allocate($line, $open);

        $this->assertSame([
            ['installment_id' => 10, 'applied_minor' => 600],
            ['installment_id' => 11, 'applied_minor' => 900],
        ], $plan['allocations']);
        $this->assertSame(0, $plan['unapplied_minor']);
    }
}
