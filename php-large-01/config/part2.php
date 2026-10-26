<?php

// Collections desk: arrears, checkout, remittance, SMS reminders, bank feed.
return [
    'portal_hosts' => array_filter(explode(',', (string) env('PART2_PORTAL_HOSTS', 'portal.lending.internal,app.lending.internal'))),

    'remittance' => [
        'callback_hosts' => array_filter(explode(',', (string) env('REMITTANCE_CALLBACK_HOSTS', 'hooks.partner-bank.internal'))),
        'signing_secret' => env('REMITTANCE_SIGNING_SECRET', 'remit-local-signing-2024'),
        'timeout' => (int) env('REMITTANCE_CALLBACK_TIMEOUT', 5),
    ],

    'short_links' => [
        'allowed_hosts' => array_filter(explode(',', (string) env('SHORT_LINK_HOSTS', 'portal.lending.internal,help.lending.internal'))),
        'code_length' => 8,
    ],

    'sms' => [
        'endpoint' => env('SMS_GATEWAY_URL', 'https://sms-gateway.sandbox.internal/v2/messages'),
        'api_key' => env('SMS_GATEWAY_KEY', 'sms-sandbox-key-2024'),
        'sender' => env('SMS_SENDER_ID', 'LENDCO'),
    ],

    'bank_feed' => [
        'base_url' => env('BANK_FEED_BASE_URL', 'https://feeds.partner-bank.internal'),
        'token' => env('BANK_FEED_TOKEN', 'bank-feed-local-2024'),
        'verify_peer' => env('BANK_FEED_VERIFY_PEER', true),
        'page_size' => (int) env('BANK_FEED_PAGE_SIZE', 200),
    ],

    'legacy_feed' => [
        'key' => env('LEGACY_FEED_KEY', 'legacy-feed-local-key-2024-000000'),
        'cipher' => env('LEGACY_FEED_CIPHER', 'aes-256-gcm'),
    ],

    'payouts' => ['max_attempts' => (int) env('PAYOUT_MAX_ATTEMPTS', 5), 'backoff_ms' => (int) env('PAYOUT_BACKOFF_MS', 250)],

    'otp' => ['max_attempts' => 5, 'window_seconds' => 900],
];
