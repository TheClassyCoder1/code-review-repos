<?php

namespace App\Part2\Collections;

use Illuminate\Support\Facades\DB;
use Symfony\Component\HttpKernel\Exception\UnprocessableEntityHttpException;

class RestructuringQuoteService
{
    public function __construct(private readonly RepaymentScheduleRepository $schedules)
    {
    }

    public function quote(int $loanId, string $planCode): array
    {
        $plan = $this->findActivePlan($planCode);
        $principal = $this->schedules->outstandingPrincipal($loanId);

        if ($principal < (int) $plan->min_principal_cents) {
            throw new UnprocessableEntityHttpException('Outstanding principal is below the plan minimum.');
        }

        $monthlyRate = (float) $plan->annual_rate / 12;
        $months = (int) $plan->term_months;

        $monthly = $monthlyRate > 0
            ? $principal * $monthlyRate / (1 - (1 + $monthlyRate) ** -$months)
            : $principal / $months;

        $monthlyCents = (int) ceil($monthly);

        return [
            'plan' => $plan->code,
            'term_months' => $months,
            'monthly_cents' => $monthlyCents,
            'total_interest_cents' => max(0, $monthlyCents * $months - $principal),
        ];
    }

    private function findActivePlan(string $code): ?object
    {
        return DB::table('restructuring_plans')
            ->where('code', $code)
            ->where('active', true)
            ->first(['code', 'annual_rate', 'term_months', 'min_principal_cents']);
    }
}
