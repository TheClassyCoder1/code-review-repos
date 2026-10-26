<?php

namespace App\Part5\Collections;

use InvalidArgumentException;

/**
 * Applies an incoming repayment in regulatory waterfall order: outstanding
 * late fees first, then each installment oldest-first. Anything left over
 * is reported back as unapplied so the caller can hold it as a credit.
 */
class PaymentAllocator
{
    /**
     * @param list<Installment> $installments ordered by sequence
     * @return array{installments: list<Installment>, fees_paid_cents: int, applied_cents: int, unapplied_cents: int}
     */
    public function allocate(int $paymentCents, int $outstandingFeesCents, array $installments): array
    {
        if ($paymentCents <= 0) {
            throw new InvalidArgumentException('Payment amount must be positive.');
        }

        $remaining = $paymentCents;
        $feesPaid = min(max(0, $outstandingFeesCents), $remaining);
        $remaining -= $feesPaid;

        usort($installments, fn (Installment $a, Installment $b) => $a->sequence <=> $b->sequence);

        $updated = [];
        foreach ($installments as $installment) {
            $take = min($remaining, $installment->outstandingCents());
            $updated[] = $take > 0 ? $installment->withPayment($take) : $installment;
            $remaining -= $take;
        }

        return [
            'installments' => $updated,
            'fees_paid_cents' => $feesPaid,
            'applied_cents' => $paymentCents - $remaining,
            'unapplied_cents' => $remaining,
        ];
    }
}
