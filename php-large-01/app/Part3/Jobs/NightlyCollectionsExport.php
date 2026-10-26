<?php

namespace App\Part3\Jobs;

use App\Part3\Exports\CollectionsCsvWriter;
use App\Part3\Models\Installment;
use Carbon\CarbonImmutable;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;

/**
 * Nightly export of open installments for the data warehouse. The scheduler
 * fires it on every app node, so a marker file in the shared lock directory
 * makes sure only one node writes the export each night.
 */
class NightlyCollectionsExport implements ShouldQueue
{
    use Dispatchable;
    use Queueable;

    public function __construct(public readonly string $businessDate)
    {
    }

    public function handle(CollectionsCsvWriter $writer): void
    {
        $date = CarbonImmutable::parse($this->businessDate);
        $marker = rtrim((string) config('part3.exports.lock_directory'), '/') . "/collections-export-{$date->format('Y-m-d')}.lock";

        if (file_exists($marker)) {
            return;
        }

        file_put_contents($marker, (string) gethostname());

        $stream = $writer->open(rtrim((string) config('part3.exports.directory'), '/') . "/collections-{$date->format('Y-m-d')}.csv");

        try {
            Installment::query()->with('plan')->whereIn('status', ['due', 'overdue'])
                ->chunkById(1000, fn ($chunk) => $chunk->each(fn ($i) => $writer->writeInstallment($stream, $i, $date)));
        } finally {
            fclose($stream);
        }
    }
}
