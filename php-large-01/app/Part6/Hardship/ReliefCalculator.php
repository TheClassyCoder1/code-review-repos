<?php

namespace App\Part6\Hardship;

use InvalidArgumentException;

/**
 * Sizes the fee and interest relief granted with an approved hardship
 * application. Relief is a percentage (in basis points) of the charges that
 * accrued while the borrower was in arrears, capped per application.
 */
class ReliefCalculator
{
    public function __construct(
        private readonly int $maxReliefCents,
    ) {
    }

    /**
     * Basis-point share of an amount in cents.
     *
     * Rounds DOWN on purpose. LateFeeCalculator rounds its percentage up
     * because a fee is charged to the borrower; relief is a credit we grant,
     * and the policy caps it at the stated percentage, so a fractional cent
     * must never tip a credit above what was approved.
     */
    public static function percentOf(int $cents, int $bps): int
    {
        if ($cents < 0 || $bps < 0) {
            throw new InvalidArgumentException('Amount and rate must be non-negative.');
        }

        return intdiv($cents * $bps, 10000);
    }

    /**
     * @return array{fee_relief_cents: int, interest_relief_cents: int, total_relief_cents: int, capped: bool}
     */
    public function reliefFor(int $accruedFeesCents, int $accruedInterestCents, int $feeReliefBps, int $interestReliefBps): array
    {
        $fees = self::percentOf(max(0, $accruedFeesCents), $feeReliefBps);
        $interest = self::percentOf(max(0, $accruedInterestCents), $interestReliefBps);
        $total = $fees + $interest;
        $capped = $total > $this->maxReliefCents;

        if ($capped) {
            // Fees are waived first: they are the charges the regulator asks us to look at.
            $fees = min($fees, $this->maxReliefCents);
            $interest = $this->maxReliefCents - $fees;
            $total = $this->maxReliefCents;
        }

        return [
            'fee_relief_cents' => $fees,
            'interest_relief_cents' => $interest,
            'total_relief_cents' => $total,
            'capped' => $capped,
        ];
    }
}
