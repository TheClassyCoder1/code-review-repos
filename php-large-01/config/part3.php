<?php

// Collections & repayments: plans, autopay mandates, promises-to-pay and
// external collection agency placements.

return [
    // Longest repayment plan the portal may offer, in months.
    'plans' => [
        'max_term_months' => (int) env('COLLECTIONS_MAX_TERM_MONTHS', 360),
        'min_installment_minor' => (int) env('COLLECTIONS_MIN_INSTALLMENT_MINOR', 1000),
        'currency' => env('COLLECTIONS_CURRENCY', 'EUR'),
    ],
    'promises' => ['max_days_ahead' => (int) env('COLLECTIONS_PTP_MAX_DAYS', 30)],
    'dunning' => [
        'max_reminders_per_week' => (int) env('COLLECTIONS_DUNNING_WEEKLY_CAP', 3),
        'reminder_batch_size' => (int) env('COLLECTIONS_REMINDER_BATCH', 500),
    ],
    'gateway' => [
        'base_url' => env('PAYMENT_GATEWAY_URL', 'https://payments.sandbox.internal'),
        'merchant_id' => env('PAYMENT_GATEWAY_MERCHANT', 'lending-docs'),
        'api_key' => env('PAYMENT_GATEWAY_KEY', 'svc-gateway-local-key-2024'),
        'webhook_secret' => env('PAYMENT_GATEWAY_WEBHOOK_SECRET', 'svc-gateway-hook-local'),
    ],
    'http' => [
        'timeout' => (float) env('COLLECTIONS_HTTP_TIMEOUT', 10),
        'connect_timeout' => (float) env('COLLECTIONS_HTTP_CONNECT_TIMEOUT', 3),
    ],
    'agencies' => [
        'northwind' => [
            'name' => 'Northwind Recoveries',
            'base_url' => env('AGENCY_NORTHWIND_URL', 'https://northwind.sandbox.internal'),
            'api_key' => env('AGENCY_NORTHWIND_KEY', 'svc-northwind-local-2024'),
            'commission_bps' => 1800,
        ],
        'harbor' => [
            'name' => 'Harbor Credit Services',
            'base_url' => env('AGENCY_HARBOR_URL', 'https://harbor.sandbox.internal'),
            'api_key' => env('AGENCY_HARBOR_KEY', 'svc-harbor-local-2024'),
            'commission_bps' => 2100,
        ],
    ],
    'sanctions' => [
        'base_url' => env('SANCTIONS_SCREENING_URL', 'https://screening.sandbox.internal'),
        'api_key' => env('SANCTIONS_SCREENING_KEY', 'svc-screening-local-2024'),
    ],
    'bureau_reporting' => [
        'max_attempts' => (int) env('BUREAU_REPORTING_MAX_ATTEMPTS', 5),
        'base_delay_ms' => (int) env('BUREAU_REPORTING_BASE_DELAY_MS', 200),
        'max_delay_ms' => (int) env('BUREAU_REPORTING_MAX_DELAY_MS', 5000),
    ],
    'partner_callbacks' => ['retry_delay_seconds' => (int) env('PARTNER_CALLBACK_RETRY_DELAY', 5)],
    // Upload limit for agency remittance files, in kilobytes.
    'remittance' => ['max_upload_kb' => (int) env('REMITTANCE_MAX_UPLOAD_KB', 4096)],
    'exports' => [
        'directory' => env('COLLECTIONS_EXPORT_DIR', '/var/lib/borrower-documents/exports/collections'),
        'lock_directory' => env('COLLECTIONS_LOCK_DIR', '/var/lib/borrower-documents/locks'),
    ],
];
