<?php

namespace App\Support;

use App\Models\MeterReading;
use RuntimeException;

class UsageCsvWriter
{
    public const COLUMNS = ['account_number', 'meter_serial', 'read_at', 'value_kwh', 'source'];

    /**
     * Create the export file and write the header row.
     *
     * @param  string  $path  Absolute path of the CSV file to create.
     * @return resource  Open handle positioned after the header.
     */
    public function open(string $path)
    {
        $handle = fopen($path, 'w');

        if ($handle === false) {
            throw new RuntimeException("Cannot open {$path} for writing");
        }

        fputcsv($handle, self::COLUMNS);

        return $handle;
    }

    /**
     * Column values for one reading, in COLUMNS order.
     *
     * @param  MeterReading  $reading  Reading with meter.customer loaded.
     * @return array<int, string>
     */
    public function formatRow(MeterReading $reading): array
    {
        $meter = $reading->meter;

        return [
            $meter->customer->account_number,
            $meter->serial,
            $reading->read_at->toIso8601String(),
            number_format((float) $reading->value, 3, '.', ''),
            $reading->source,
        ];
    }
}
