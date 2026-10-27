<?php

namespace App\Console\Commands;

use App\Models\MeterReading;
use App\Support\UsageCsvWriter;
use Carbon\CarbonImmutable;
use Illuminate\Console\Command;

class ExportUsageCommand extends Command
{
    protected $signature = 'billing:export-usage {month : Month to export, YYYY-MM}';

    protected $description = 'Export all meter readings for a month to CSV for the regulator return';

    public function handle(UsageCsvWriter $writer): int
    {
        $month = CarbonImmutable::createFromFormat('!Y-m', (string) $this->argument('month'));

        if ($month === false) {
            $this->error('Month must be in YYYY-MM format.');

            return self::INVALID;
        }

        $path = storage_path('exports/usage-'.$month->format('Y-m').'.csv');
        $handle = $writer->open($path);

        try {
            MeterReading::with('meter.customer')
                ->whereBetween('read_at', [$month->startOfMonth(), $month->endOfMonth()])
                ->chunkById(1000, function ($readings) use ($handle, $writer) {
                    foreach ($readings as $reading) {
                        fputcsv($handle, $writer->formatRow($reading));
                    }
                });
        } finally {
            fclose($handle);
        }

        $this->info("Usage written to {$path}");

        return self::SUCCESS;
    }
}
