<?php

/*
|--------------------------------------------------------------------------
| Disbursement / payout rail settings
|--------------------------------------------------------------------------
|
| export_root: where bulk-payment CSVs are written before the bank's
| host-to-host agent picks them up.
|
| rail: the sponsor bank's real-time payout API (IMPS / SEPA Instant). Every
| HTTP client for the rail is built by PayoutHttpClientFactory from these
| values; requests inherit the timeouts below.
|
*/

return [
    'export_root' => env('PAYOUT_EXPORT_ROOT', '/var/lib/borrower-documents/disbursements'),

    'rail' => [
        'base_url' => env('PAYOUT_RAIL_BASE_URL', 'https://payouts.sandbox.internal'),
        'api_key' => env('PAYOUT_RAIL_API_KEY', 'payout-rail-local-key'),
        'connect_timeout' => (float) env('PAYOUT_RAIL_CONNECT_TIMEOUT', 3),
        'timeout' => (float) env('PAYOUT_RAIL_TIMEOUT', 10),
        'read_timeout' => (float) env('PAYOUT_RAIL_READ_TIMEOUT', 10),
    ],

    'max_instant_minor' => (int) env('PAYOUT_MAX_INSTANT_MINOR', 50000000),
];
