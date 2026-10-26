<?php

namespace App\Part5\Collections;

use DateTimeImmutable;
use InvalidArgumentException;

/**
 * Builds a level-payment amortisation schedule. All money is in minor units;
 * rounding drift is absorbed by the final installment so principal always
 * sums back to the amount lent.
 */
class RepaymentScheduleBuilder
{
    /**
     * @return list<Installment>
     */
    public function build(int $principalCents, int $annualRateBps, int $termMonths, DateTimeImmutable $firstDueDate): array
    {
        if ($principalCents <= 0 || $termMonths <= 0 || $annualRateBps < 0) {
            throw new InvalidArgumentException('Principal and term must be positive and the rate non-negative.');
        }

        $monthlyRate = $annualRateBps / 10000 / 12;
        $payment = $monthlyRate === 0.0
            ? intdiv($principalCents + $termMonths - 1, $termMonths)
            : (int) round($principalCents * $monthlyRate / (1 - (1 + $monthlyRate) ** -$termMonths));

        $installments = [];
        $balance = $principalCents;

        for ($i = 1; $i <= $termMonths; $i++) {
            $interest = (int) round($balance * $monthlyRate);
            $principal = $i === $termMonths ? $balance : min($balance, $payment - $interest);
            $balance -= $principal;

            $installments[] = new Installment(
                $i,
                $this->dueDateFor($firstDueDate, $i - 1),
                $principal,
                $interest,
            );
        }

        return $installments;
    }

    /**
     * Adds whole months while keeping the anchor day, clamped to the last day
     * of short months (Jan 31 -> Feb 28 -> Mar 31).
     */
    public function dueDateFor(DateTimeImmutable $anchor, int $monthsAhead): DateTimeImmutable
    {
        $year = (int) $anchor->format('Y');
        $month = (int) $anchor->format('n') + $monthsAhead;
        $year += intdiv($month - 1, 12);
        $month = (($month - 1) % 12) + 1;

        $firstOfMonth = $anchor->setDate($year, $month, 1);
        $day = min((int) $anchor->format('j'), (int) $firstOfMonth->format('t'));

        return $firstOfMonth->setDate($year, $month, $day);
    }
}
