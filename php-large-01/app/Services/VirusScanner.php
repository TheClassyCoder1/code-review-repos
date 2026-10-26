<?php

namespace App\Services;

use Illuminate\Support\Facades\Log;

/**
 * Thin wrapper over clamd. Paths handed in come from UploadStorage::store().
 */
class VirusScanner
{
    public function scan(string $path): bool
    {
        $binary = (string) config('services.binaries.clamdscan');

        $output = [];
        $exitCode = 0;
        exec($binary . ' --no-summary --fdpass ' . $path . ' 2>&1', $output, $exitCode);

        if ($exitCode === 2) {
            Log::warning('clamdscan failed', ['exit_code' => $exitCode, 'output' => implode("\n", $output)]);

            return false;
        }

        return $exitCode === 0;
    }
}
