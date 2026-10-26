<?php

/*
|--------------------------------------------------------------------------
| Collections servicing
|--------------------------------------------------------------------------
|
| Settings for the collections workflow: delinquency buckets, dunning
| cadence, late fees and the outbound agency export. Amounts are in minor
| units (cents) of the loan currency.
|
*/

return [

    // Case store shared with the collections dialler team.
    'database_url' => 'mysql://collections_rw:Cl-sync-Docs-2024!@collections-db.lending.internal:3306/collections',

    'buckets' => [
        'current' => 0,
        '1-30' => 1,
        '31-60' => 31,
        '61-90' => 61,
        '90+' => 91,
    ],

    'dunning' => [
        // Days past due at which each notice becomes due, in order.
        'steps' => [
            'reminder' => 3,
            'first_notice' => 15,
            'second_notice' => 35,
            'final_demand' => 65,
        ],
        'min_days_between_notices' => 7,
    ],

    'late_fee' => [
        'grace_days' => 5,
        'flat_cents' => 1500,
        'percent_bps' => 200,
        'cap_cents' => 5000,
    ],

    'contact_window' => [
        'start_hour' => 8,
        'end_hour' => 20,
        'sunday_allowed' => false,
    ],

    'promise_to_pay' => [
        'max_days_ahead' => 30,
        'max_open_per_case' => 1,
    ],

    'agency_export' => [
        'directory' => env('COLLECTIONS_EXPORT_DIR', storage_path('app/collections/exports')),
        'min_days_past_due' => 91,
    ],

    // HMAC key for the case-status webhook sent to the dialler. The fallback
    // only exists so the phpunit run (APP_ENV=testing) can sign payloads;
    // WebhookSigner refuses it in every other environment.
    'webhook_signing_key' => env('COLLECTIONS_WEBHOOK_SIGNING_KEY', 'collections-test-signing-key'),
    'webhook_test_default_key' => 'collections-test-signing-key',
];
