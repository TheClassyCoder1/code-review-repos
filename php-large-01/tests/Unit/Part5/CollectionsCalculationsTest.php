<?php

namespace Tests\Unit\Part5;

use App\Part5\Collections\AgencyCredentials;
use App\Part5\Collections\DelinquencyClassifier;
use App\Part5\Collections\HardshipPlanCalculator;
use App\Part5\Collections\Installment;
use App\Part5\Collections\LateFeeCalculator;
use App\Part5\Collections\PaymentAllocator;
use App\Part5\Collections\RepaymentScheduleBuilder;
use App\Part5\Collections\WebhookSigner;
use DateTimeImmutable;
use InvalidArgumentException;
use PHPUnit\Framework\TestCase;
use RuntimeException;
use Tests\Unit\Part5\Fixtures\AgencyFixtures;

class CollectionsCalculationsTest extends TestCase
{
    public function test_schedule_principal_sums_to_amount_lent(): void
    {
        $schedule = (new RepaymentScheduleBuilder())->build(1_000_000, 1250, 24, new DateTimeImmutable('2026-01-31'));

        $this->assertCount(24, $schedule);
        $this->assertSame(1_000_000, array_sum(array_map(fn (Installment $i) => $i->principalCents, $schedule)));
        $this->assertSame('2026-02-28', $schedule[1]->dueDate->format('Y-m-d'));
        $this->assertSame('2026-03-31', $schedule[2]->dueDate->format('Y-m-d'));
    }

    public function test_zero_rate_schedule_has_no_interest(): void
    {
        $schedule = (new RepaymentScheduleBuilder())->build(1000, 0, 3, new DateTimeImmutable('2026-05-10'));

        $this->assertSame([334, 334, 332], array_map(fn (Installment $i) => $i->principalCents, $schedule));
        $this->assertSame(0, array_sum(array_map(fn (Installment $i) => $i->interestCents, $schedule)));
    }

    public function test_classifier_uses_oldest_unpaid_installment(): void
    {
        $classifier = new DelinquencyClassifier(['current' => 0, '1-30' => 1, '31-60' => 31, '61-90' => 61, '90+' => 91]);
        $installments = [
            new Installment(1, new DateTimeImmutable('2026-08-01'), 9000, 1000, 10000),
            new Installment(2, new DateTimeImmutable('2026-09-01'), 9100, 900),
            new Installment(3, new DateTimeImmutable('2026-10-01'), 9200, 800),
            new Installment(4, new DateTimeImmutable('2026-11-01'), 9300, 700),
        ];

        $status = $classifier->classify($installments, new DateTimeImmutable('2026-10-15 13:00'));

        $this->assertSame(44, $status['days_past_due']);
        $this->assertSame('31-60', $status['bucket']);
        $this->assertSame(20000, $status['arrears_cents']);
    }

    public function test_allocator_pays_fees_then_oldest_installment(): void
    {
        $result = (new PaymentAllocator())->allocate(12000, 1500, [
            new Installment(2, new DateTimeImmutable('2026-09-01'), 9000, 1000),
            new Installment(1, new DateTimeImmutable('2026-08-01'), 9000, 1000),
        ]);

        $this->assertSame(1500, $result['fees_paid_cents']);
        $this->assertTrue($result['installments'][0]->isSettled());
        $this->assertSame(1, $result['installments'][0]->sequence);
        $this->assertSame(500, $result['installments'][1]->paidCents);
        $this->assertSame(0, $result['unapplied_cents']);
    }

    public function test_late_fee_respects_grace_and_cap(): void
    {
        $calculator = new LateFeeCalculator(5, 1500, 200, 5000);
        $installment = new Installment(1, new DateTimeImmutable('2026-10-01'), 400000, 20000);

        $this->assertSame(0, $calculator->feeFor($installment, new DateTimeImmutable('2026-10-06'), false));
        $this->assertSame(5000, $calculator->feeFor($installment, new DateTimeImmutable('2026-10-07'), false));
        $this->assertSame(0, $calculator->feeFor($installment, new DateTimeImmutable('2026-10-07'), true));
    }

    public function test_hardship_plan_never_drops_below_interest(): void
    {
        $plan = (new HardshipPlanCalculator())->propose(1_200_000, 60000, 1200, 5000, 6);

        $this->assertSame(12000, $plan['monthly_cents']);
        $this->assertSame(0, $plan['arrears_catch_up_cents']);
        $this->assertFalse($plan['within_affordability']);
    }

    public function test_webhook_signature_round_trip(): void
    {
        $signer = new WebhookSigner('k-unit', 'collections-test-signing-key', 'testing', 'https://dialler.test/hook');
        $header = $signer->sign('{"event":"case.closed"}', 1_790_000_000);

        $this->assertTrue($signer->verify('{"event":"case.closed"}', $header, 1_790_000_100));
        $this->assertFalse($signer->verify('{"event":"case.opened"}', $header, 1_790_000_100));
        $this->assertFalse($signer->verify('{"event":"case.closed"}', $header, 1_790_001_000));
    }

    public function test_webhook_signer_rejects_test_key_in_production(): void
    {
        $this->expectException(RuntimeException::class);

        new WebhookSigner('collections-test-signing-key', 'collections-test-signing-key', 'production', 'https://dialler.test/hook');
    }

    public function test_agency_credentials_reject_placeholder_password(): void
    {
        $this->expectException(InvalidArgumentException::class);

        AgencyCredentials::fromArray(AgencyFixtures::dropHostConfig());
    }
}
