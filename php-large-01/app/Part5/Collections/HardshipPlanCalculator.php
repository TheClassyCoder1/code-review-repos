<?php

namespace App\Part5\Collections;

use InvalidArgumentException;

/**
 * Sizes a temporary reduced-payment plan for a borrower in hardship. The
 * reduced installment is bounded by what the borrower can afford, but it
 * must at least cover the monthly interest so the balance does not grow.
 */
class HardshipPlanCalculator
{
    private const MAX_PLAN_MONTHS = 12;

    /**
     * @return array{monthly_cents: int, months: int, arrears_catch_up_cents: int, within_affordability: bool}
     */
    public function propose(int $balanceCents, int $arrearsCents, int $annualRateBps, int $affordableMonthlyCents, int $requestedMonths): array
    {
        if ($balanceCents <= 0 || $affordableMonthlyCents <= 0) {
            throw new InvalidArgumentException('Balance and affordable amount must be positive.');
        }

        $months = max(1, min($requestedMonths, self::MAX_PLAN_MONTHS));
        $monthlyInterest = (int) ceil($balanceCents * $annualRateBps / 10000 / 12);

        $catchUp = intdiv(max(0, $arrearsCents) + $months - 1, $months);
        $target = $monthlyInterest + $catchUp;
        $monthly = max($monthlyInterest, min($target, $affordableMonthlyCents));

        return [
            'monthly_cents' => $monthly,
            'months' => $months,
            'arrears_catch_up_cents' => $monthly - $monthlyInterest,
            'within_affordability' => $monthly <= $affordableMonthlyCents,
        ];
    }
}
