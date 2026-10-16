<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Http;

class MeterVendorClient
{
    /**
     * Latest register reading the vendor holds for a smart meter.
     *
     * @param  string  $vendorMeterId  The vendor's identifier for the meter.
     * @return array{value: float, read_at: Carbon}|null  Null when the vendor has no reading yet.
     */
    public function latestReading(string $vendorMeterId): ?array
    {
        $response = Http::meterVendor()
            ->get('/v1/meters/'.rawurlencode($vendorMeterId).'/readings/latest');

        if ($response->status() === 404) {
            return null;
        }

        $response->throw();

        return [
            'value' => (float) $response->json('value'),
            'read_at' => Carbon::parse($response->json('read_at')),
        ];
    }
}
