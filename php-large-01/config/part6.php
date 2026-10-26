<?php

/*
|--------------------------------------------------------------------------
| Hardship intake and borrower contact
|--------------------------------------------------------------------------
|
| Amounts are in minor units (cents) of the servicing currency.
|
*/

return [

    'currency' => env('HARDSHIP_CURRENCY', 'EUR'),

    'relief' => [
        // Share of fees / interest accrued in arrears that an approved application waives.
        'fee_bps' => (int) env('HARDSHIP_FEE_RELIEF_BPS', 10000),
        'interest_bps' => (int) env('HARDSHIP_INTEREST_RELIEF_BPS', 5000),
        'max_cents' => (int) env('HARDSHIP_MAX_RELIEF_CENTS', 150000),
    ],

    'funnel_analytics' => [
        'enabled' => (bool) env('FUNNEL_ANALYTICS_ENABLED', false),
        'endpoint' => env('FUNNEL_ANALYTICS_ENDPOINT', 'https://events.product-analytics.internal/v1/batch'),
        'write_key' => env('FUNNEL_ANALYTICS_WRITE_KEY', ''),
    ],

    'contact_engagement' => [
        'endpoint' => env('CONTACT_ENGAGEMENT_ENDPOINT', 'https://api.reachscore.io/v2/profiles/upsert'),
        'api_key' => env('CONTACT_ENGAGEMENT_API_KEY', 'svc-reachscore-key-2024'),
    ],
];
