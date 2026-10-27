<?php

return [

    'currency' => env('BILLING_CURRENCY', 'GBP'),

    'vat_basis_points' => (int) env('BILLING_VAT_BASIS_POINTS', 500),

    'late_fee_rate' => 0.015,

    'payment_terms_days' => 21,

    'vendor' => [
        'base_url' => env('METER_VENDOR_URL', 'https://api.meterhub.example'),
        'token' => env('METER_VENDOR_TOKEN'),
        'webhook_token' => env('METER_VENDOR_WEBHOOK_TOKEN'),
        'timeout' => (int) env('METER_VENDOR_TIMEOUT', 5),
        'connect_timeout' => (int) env('METER_VENDOR_CONNECT_TIMEOUT', 2),
    ],

    'gateway' => [
        'base_url' => env('PAYMENT_GATEWAY_URL', 'https://api.paygate.example'),
        'secret' => env('PAYMENT_GATEWAY_SECRET'),
        'webhook_secret' => env('PAYMENT_GATEWAY_WEBHOOK_SECRET'),
        'timeout' => (int) env('PAYMENT_GATEWAY_TIMEOUT', 10),
        'connect_timeout' => (int) env('PAYMENT_GATEWAY_CONNECT_TIMEOUT', 3),
    ],

    'portal' => [
        'token_key' => env('PORTAL_TOKEN_KEY'),
    ],

];
