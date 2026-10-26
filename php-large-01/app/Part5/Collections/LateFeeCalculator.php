<?php

namespace App\Part5\Collections;

use DateTimeImmutable;

/**
 * Late fee for a single missed installment: the greater of a flat amount or
 * a percentage of the overdue balance, capped, and only once the grace
 * period has fully elapsed. A fee is never larger than the overdue amount.
 */
class LateFeeCalculator
{
    public function __construct(
        private readonly int $graceDays,
        private readonly int $flatCents,
        private readonly int $percentBps,
        private readonly int $capCents,
    ) {
    }

    public function feeFor(Installment $installment, DateTimeImmutable $asOf, bool $feeAlreadyCharged): int
    {
        if ($feeAlreadyCharged || $installment->isSettled()) {
            return 0;
        }

        $due = $installment->dueDate->setTime(0, 0);
        $today = $asOf->setTime(0, 0);

        if ($today <= $due) {
            return 0;
        }

        $daysLate = (int) $due->diff($today)->days;
        if ($daysLate <= $this->graceDays) {
            return 0;
        }

        $overdue = $installment->outstandingCents();
        $percentFee = intdiv($overdue * $this->percentBps + 9999, 10000);
        $fee = max($this->flatCents, $percentFee);

        return min($fee, $this->capCents, $overdue);
    }
}
