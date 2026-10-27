<?php

namespace App\Jobs;

use App\Models\Meter;
use App\Models\MeterReading;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use RuntimeException;

class ImportMeterReadingsJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable;

    public const HEADER = ['meter_serial', 'read_at', 'value_kwh'];

    public int $tries = 3;

    public function __construct(public string $relativePath)
    {
    }

    /**
     * Import a vendor CSV drop of cumulative meter readings.
     *
     * @return void
     */
    public function handle(): void
    {
        $path = Storage::disk('imports')->path($this->relativePath);
        $handle = fopen($path, 'r');

        if ($handle === false) {
            throw new RuntimeException("Unable to open reading import {$this->relativePath}");
        }

        $header = fgetcsv($handle);

        if ($header !== self::HEADER) {
            Log::warning('Reading import has an unexpected header', ['path' => $this->relativePath]);

            return;
        }

        $imported = 0;

        while (($row = fgetcsv($handle)) !== false) {
            $this->persistRow(array_combine(self::HEADER, $row));
            $imported++;
        }

        Log::info('Reading import finished', ['path' => $this->relativePath, 'rows' => $imported]);
    }

    /**
     * @param  array{meter_serial: string, read_at: string, value_kwh: string}  $row
     */
    private function persistRow(array $row): void
    {
        DB::transaction(function () use ($row) {
            $meter = Meter::where('serial', $row['meter_serial'])->lockForUpdate()->firstOrFail();
            $readAt = Carbon::parse($row['read_at']);

            MeterReading::firstOrCreate(
                ['meter_id' => $meter->id, 'read_at' => $readAt],
                ['value' => $row['value_kwh'], 'source' => MeterReading::SOURCE_VENDOR],
            );

            $meter->advanceRegister((float) $row['value_kwh'], $readAt);
        });
    }
}
