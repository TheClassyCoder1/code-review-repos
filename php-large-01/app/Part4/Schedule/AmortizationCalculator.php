<?php

namespace App\Part4\Schedule;

use DateTimeImmutable;
use InvalidArgumentException;

/**
 * Builds level-payment (EMI) schedules. Interest is computed per period on the
 * outstanding balance and rounded half-up to the minor unit; the final
 * installment absorbs any rounding drift so the balance closes at exactly zero.
 */
class AmortizationCalculator
{
    /**
     * @return list<Installment>
     */
    public function equalInstallmentSchedule(
        int $principalMinor,
        int $annualRateBasisPoints,
        int $termMonths,
        DateTimeImmutable $firstDueOn,
    ): array {
        if ($principalMinor <= 0 || $termMonths <= 0 || $annualRateBasisPoints < 0) {
            throw new InvalidArgumentException('Principal and term must be positive and the rate non-negative.');
        }

        $monthlyRate = $annualRateBasisPoints / 10000 / 12;
        $payment = $this->levelPaymentMinor($principalMinor, $monthlyRate, $termMonths);
        $balance = $principalMinor;
        $installments = [];

        for ($sequence = 1; $sequence <= $termMonths; $sequence++) {
            $interest = (int) round($balance * $monthlyRate, 0, PHP_ROUND_HALF_UP);
            $principal = $sequence === $termMonths ? $balance : min($balance, $payment - $interest);
            $balance -= $principal;

            $installments[] = new Installment(
                $sequence,
                $this->dueDateFor($firstDueOn, $sequence - 1),
                $principal,
                $interest,
                $balance,
            );
        }

        return $installments;
    }

    public function levelPaymentMinor(int $principalMinor, float $monthlyRate, int $termMonths): int
    {
        if ($monthlyRate === 0.0) {
            return (int) ceil($principalMinor / $termMonths);
        }

        $factor = (1 + $monthlyRate) ** $termMonths;

        return (int) round($principalMinor * $monthlyRate * $factor / ($factor - 1), 0, PHP_ROUND_HALF_UP);
    }

    /**
     * Adds whole months to the first due date, clamping to the last day of the
     * target month instead of letting PHP roll 31 January into March.
     */
    public function dueDateFor(DateTimeImmutable $firstDueOn, int $monthsAhead): DateTimeImmutable
    {
        $day = (int) $firstDueOn->format('j');
        $targetMonthStart = $firstDueOn->modify('first day of this month')->modify("+{$monthsAhead} months");
        $lastDay = (int) $targetMonthStart->format('t');

        return $targetMonthStart->setDate(
            (int) $targetMonthStart->format('Y'),
            (int) $targetMonthStart->format('n'),
            min($day, $lastDay),
        );
    }

    /**
     * Interest accrued between two dates on an Actual/365 basis, used when a
     * borrower prepays mid-period.
     */
    public function accruedInterestMinor(
        int $balanceMinor,
        int $annualRateBasisPoints,
        DateTimeImmutable $from,
        DateTimeImmutable $to,
    ): int {
        if ($to <= $from || $balanceMinor <= 0) {
            return 0;
        }

        $days = (int) $from->diff($to)->days;
        $accrued = $balanceMinor * $annualRateBasisPoints * $days / (10000 * 365);

        return (int) round($accrued, 0, PHP_ROUND_HALF_UP);
    }
}
