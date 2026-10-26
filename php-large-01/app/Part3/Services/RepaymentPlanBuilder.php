<?php

namespace App\Part3\Services;

use App\Part3\Models\RepaymentPlan;
use App\Part3\Repositories\InstallmentRepository;
use App\Part3\Repositories\PlanRepository;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;
use InvalidArgumentException;

/**
 * Turns an arrears balance into a fixed-term repayment plan with an
 * annuity schedule (one installment row per month of the term).
 */
class RepaymentPlanBuilder
{
    public function __construct(
        private readonly PlanRepository $plans,
        private readonly InstallmentRepository $installments,
    ) {
    }

    public function build(int $borrowerId, string $loanReference, int $principalMinor, int $annualRateBps, int $termMonths, CarbonImmutable $firstDueOn): RepaymentPlan
    {
        $maxTerm = (int) config('part3.plans.max_term_months', 360);

        if ($termMonths < 1 || $termMonths > $maxTerm) {
            throw new InvalidArgumentException("Plan term must be between 1 and {$maxTerm} months.");
        }

        if ($principalMinor <= 0 || $annualRateBps < 0 || $annualRateBps > 10000) {
            throw new InvalidArgumentException('Plan principal or rate is out of range.');
        }

        $rate = $annualRateBps / 10000 / 12;
        $payment = (int) round($rate === 0.0
            ? $principalMinor / $termMonths
            : $principalMinor * $rate / (1 - (1 + $rate) ** -$termMonths));

        if ($payment < (int) config('part3.plans.min_installment_minor', 1000)) {
            throw new InvalidArgumentException('Installment would fall below the minimum amount.');
        }

        return DB::transaction(function () use ($borrowerId, $loanReference, $principalMinor, $annualRateBps, $termMonths, $firstDueOn, $rate, $payment) {
            $plan = $this->plans->create([
                'borrower_id' => $borrowerId,
                'loan_reference' => $loanReference,
                'principal_minor' => $principalMinor,
                'annual_rate_bps' => $annualRateBps,
                'term_months' => $termMonths,
                'currency' => (string) config('part3.plans.currency', 'EUR'),
                'status' => 'active',
            ]);

            $now = now();
            $balance = $principalMinor;
            $rows = [];

            for ($month = 1; $month <= $termMonths; $month++) {
                $interest = (int) round($balance * $rate);
                $amount = $month === $termMonths ? $balance + $interest : $payment;
                $balance -= $amount - $interest;
                $rows[] = [
                    'repayment_plan_id' => $plan->id,
                    'sequence' => $month,
                    'due_on' => $firstDueOn->addMonthsNoOverflow($month - 1)->format('Y-m-d'),
                    'amount_minor' => $amount,
                    'created_at' => $now,
                    'updated_at' => $now,
                ];
            }
            $this->installments->insertSchedule($rows);

            return $plan;
        });
    }
}
