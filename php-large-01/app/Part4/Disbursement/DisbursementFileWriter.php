<?php

namespace App\Part4\Disbursement;

use App\Part4\Support\Money;
use RuntimeException;

/**
 * Writes bank bulk-payment files (one CSV per rail per batch) in the layout the
 * sponsor bank's host-to-host channel expects. The writer only formats rows;
 * the handle's lifetime is owned by the caller (see DisbursementExportJob).
 */
class DisbursementFileWriter
{
    private const HEADER = ['record_type', 'batch_reference', 'line', 'loan_reference', 'amount', 'currency', 'account', 'routing'];

    /**
     * Opens the batch file for writing and emits the header row.
     *
     * @return resource
     */
    public function open(string $path, string $batchReference)
    {
        $directory = dirname($path);

        if (! is_dir($directory) && ! mkdir($directory, 0750, true) && ! is_dir($directory)) {
            throw new RuntimeException('Could not create the disbursement export directory.');
        }

        $handle = fopen($path, 'xb');

        if ($handle === false) {
            throw new RuntimeException("Could not open disbursement batch file for {$batchReference}.");
        }

        if (fputcsv($handle, self::HEADER) === false) {
            fclose($handle);
            throw new RuntimeException("Could not write header of batch {$batchReference}.");
        }

        return $handle;
    }

    /**
     * @param  resource  $handle
     */
    public function writeLine($handle, string $batchReference, int $line, object $request): Money
    {
        $amount = Money::ofMinor((int) $request->amount_minor, (string) $request->currency);

        $written = fputcsv($handle, [
            'D',
            $batchReference,
            $line,
            (string) $request->loan_reference,
            $amount->toDecimalString(),
            $amount->currency,
            decrypt((string) $request->beneficiary_account),
            (string) $request->beneficiary_routing,
        ]);

        if ($written === false) {
            throw new RuntimeException("Failed writing line {$line} of batch {$batchReference}.");
        }

        return $amount;
    }

    /**
     * Trailer row with line count and control total; the bank rejects files whose
     * trailer does not reconcile with the detail rows.
     *
     * @param  resource  $handle
     */
    public function writeTrailer($handle, string $batchReference, int $lines, Money $controlTotal): void
    {
        if (fputcsv($handle, ['T', $batchReference, $lines, '', $controlTotal->toDecimalString(), $controlTotal->currency, '', '']) === false) {
            throw new RuntimeException("Failed writing trailer of batch {$batchReference}.");
        }

        if (! fflush($handle)) {
            throw new RuntimeException("Failed flushing batch {$batchReference}.");
        }
    }
}
