<?php

namespace App\Part2\Collections;

use RuntimeException;
use UnexpectedValueException;

class ArrearsCsvExporter
{
    private const HEADER = ['loan_id', 'borrower_id', 'arrears_cents', 'days_past_due', 'bucket', 'last_payment_at'];

    public function export(iterable $rows, string $fileName): string
    {
        $dir = rtrim((string) config('services.storage.root'), '/') . '/' . config('services.storage.exports');
        $path = $dir . '/' . basename($fileName);

        $handle = fopen($path, 'wb');

        if ($handle === false) {
            throw new RuntimeException('Could not open export file for writing.');
        }

        fputcsv($handle, self::HEADER);

        foreach ($rows as $row) {
            if (! in_array($row->bucket, ArrearsAccount::BUCKETS, true)) {
                throw new UnexpectedValueException('Unknown arrears bucket in export row.');
            }

            fputcsv($handle, [
                (int) $row->loan_id,
                (int) $row->borrower_id,
                (int) $row->arrears_cents,
                (int) $row->days_past_due,
                $row->bucket,
                $row->last_payment_at ?? '',
            ]);
        }

        fclose($handle);

        return $path;
    }
}
