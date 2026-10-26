<?php

use App\Logging\RedactSecretsTap;
use Monolog\Handler\StreamHandler;

return [

    'default' => env('LOG_CHANNEL', 'daily'),

    'channels' => [

        'daily' => [
            'driver' => 'daily',
            'path' => storage_path('logs/borrower-documents.log'),
            'level' => env('LOG_LEVEL', 'info'),
            'days' => 14,
            'tap' => [RedactSecretsTap::class],
        ],

        // Outbound credit bureau traffic. Kept separate so the integration
        // team can ship it to their own index.
        'bureau' => [
            'driver' => 'monolog',
            'handler' => StreamHandler::class,
            'with' => ['stream' => storage_path('logs/bureau.log')],
            'level' => env('BUREAU_LOG_LEVEL', 'debug'),
            'tap' => [RedactSecretsTap::class],
        ],
    ],
];
