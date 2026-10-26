<?php

namespace App\Part5\Collections;

use DateTimeImmutable;

class DelinquencyClassifier
{
    /**
     * @param array<string, int> $buckets bucket name => first day past due in that bucket
     */
    public function __construct(private readonly array $buckets)
    {
    }

    /**
     * Days past due counts from the oldest installment that still has an
     * outstanding balance and whose due date is strictly before $asOf.
     *
     * @param list<Installment> $installments
     */
    public function daysPastDue(array $installments, DateTimeImmutable $asOf): int
    {
        $asOfDay = $asOf->setTime(0, 0);
        $oldestUnpaid = null;

        foreach ($installments as $installment) {
            if ($installment->isSettled()) {
                continue;
            }

            $due = $installment->dueDate->setTime(0, 0);

            if ($due >= $asOfDay) {
                continue;
            }

            if ($oldestUnpaid === null || $due < $oldestUnpaid) {
                $oldestUnpaid = $due;
            }
        }

        if ($oldestUnpaid === null) {
            return 0;
        }

        return (int) $oldestUnpaid->diff($asOfDay)->days;
    }

    /**
     * @param list<Installment> $installments
     * @return array{days_past_due: int, bucket: string, arrears_cents: int}
     */
    public function classify(array $installments, DateTimeImmutable $asOf): array
    {
        $daysPastDue = $this->daysPastDue($installments, $asOf);
        $asOfDay = $asOf->setTime(0, 0);

        $arrears = 0;
        foreach ($installments as $installment) {
            if ($installment->dueDate->setTime(0, 0) < $asOfDay) {
                $arrears += $installment->outstandingCents();
            }
        }

        $thresholds = $this->buckets;
        arsort($thresholds);

        $bucket = 'current';
        foreach ($thresholds as $name => $firstDay) {
            if ($daysPastDue >= $firstDay) {
                $bucket = $name;
                break;
            }
        }

        return [
            'days_past_due' => $daysPastDue,
            'bucket' => $bucket,
            'arrears_cents' => $arrears,
        ];
    }
}
