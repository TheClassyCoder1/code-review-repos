<?php

namespace App\Part3\Exports;

use App\Part3\Models\Installment;
use DateTimeInterface;
use RuntimeException;

/**
 * CSV layout shared by the arrears report and the nightly collections
 * export. open() hands the stream to the caller, which writes rows and is
 * responsible for closing it.
 */
class CollectionsCsvWriter
{
    public const HEADER = ['plan_id', 'installment_id', 'loan_reference', 'due_on', 'amount_minor', 'paid_minor', 'days_overdue', 'status'];

    /**
     * @return resource
     */
    public function open(string $path)
    {
        if (! is_dir(dirname($path)) && ! mkdir(dirname($path), 0750, true) && ! is_dir(dirname($path))) {
            throw new RuntimeException('Export directory could not be created.');
        }

        $stream = fopen($path, 'wb') ?: throw new RuntimeException('Export file could not be opened for writing.');
        fputcsv($stream, self::HEADER);

        return $stream;
    }

    /**
     * @param resource $stream
     */
    public function writeInstallment($stream, Installment $installment, DateTimeInterface $asOf): void
    {
        fputcsv($stream, [
            $installment->repayment_plan_id,
            $installment->id,
            $installment->plan?->loan_reference,
            $installment->due_on->format('Y-m-d'),
            $installment->amount_minor,
            $installment->paid_minor,
            max(0, (int) $installment->due_on->diffInDays($asOf, false)),
            $installment->status,
        ]);
    }
}
