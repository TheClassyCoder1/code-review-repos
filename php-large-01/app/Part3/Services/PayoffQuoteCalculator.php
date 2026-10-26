<?php

namespace App\Part3\Services;

use InvalidArgumentException;

/**
 * "What if I pay X a month?" calculator on the portal arrears page: how
 * long the payoff takes and what interest it costs.
 */
class PayoffQuoteCalculator
{
    /**
     * @return array{months: int, total_interest_minor: int}
     */
    public function quote(int $balanceMinor, int $annualRateBps, int $monthlyPaymentMinor): array
    {
        if ($balanceMinor <= 0 || $monthlyPaymentMinor <= 0) {
            throw new InvalidArgumentException('Balance and monthly payment must be positive.');
        }

        $monthlyRate = $annualRateBps / 10000 / 12;
        $balance = $balanceMinor;
        $months = 0;
        $totalInterest = 0;

        while ($balance > 0) {
            $interest = (int) round($balance * $monthlyRate);
            $totalInterest += $interest;
            $balance = $balance + $interest - min($monthlyPaymentMinor, $balance + $interest);
            $months++;
        }

        return ['months' => $months, 'total_interest_minor' => $totalInterest];
    }
}
