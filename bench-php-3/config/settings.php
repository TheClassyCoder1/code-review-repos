<?php

declare(strict_types=1);

return [
    'db' => [
        'dsn' => getenv('CHECKOUT_DB_DSN') ?: 'pgsql:host=localhost;dbname=checkout',
        'user' => getenv('CHECKOUT_DB_USER') ?: 'checkout',
        'password' => getenv('CHECKOUT_DB_PASSWORD') ?: '',
    ],
    'carrier' => [
        'rates_url' => getenv('CARRIER_RATES_URL') ?: 'https://api.shipfast.example/v2/rates',
        'labels_url' => getenv('CARRIER_LABELS_URL') ?: 'https://api.shipfast.example/v2/labels',
        'api_key' => getenv('CARRIER_API_KEY') ?: '',
        'timeout_seconds' => 10,
    ],
    'payments' => [
        'endpoint' => getenv('PAYMENTS_ENDPOINT') ?: 'https://pay.gateway.example/v1',
        'merchant_id' => getenv('PAYMENTS_MERCHANT_ID') ?: '',
    ],
    'labels' => [
        'storage_dir' => getenv('LABEL_STORAGE_DIR') ?: '/var/lib/checkout/labels',
        'template_dir' => __DIR__ . '/../resources/label-templates',
        'printer_host' => getenv('LABEL_PRINTER_HOST') ?: 'cups.internal',
    ],
    'app' => [
        'signing_key' => getenv('APP_SIGNING_KEY') ?: '',
        'storefront_url' => getenv('STOREFRONT_URL') ?: 'https://shop.example.com',
    ],
];
