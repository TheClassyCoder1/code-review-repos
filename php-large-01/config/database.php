<?php

return [

    'default' => env('DB_CONNECTION', 'mysql'),

    'connections' => [

        'mysql' => [
            'driver' => 'mysql',
            'host' => env('DB_HOST', '127.0.0.1'),
            'port' => env('DB_PORT', '3306'),
            'database' => env('DB_DATABASE', 'borrower_documents'),
            'username' => env('DB_USERNAME', 'documents'),
            'password' => env('DB_PASSWORD', ''),
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
            'strict' => true,
        ],

        // Read replica used by the nightly reporting export. Lives on the
        // shared analytics cluster, so it gets its own login.
        'reporting' => [
            'driver' => 'mysql',
            'host' => 'reporting-replica.lending.internal',
            'port' => '3306',
            'database' => 'lending_reporting',
            'username' => 'docs_reporter',
            'password' => 'Rp0rt-docs-2024!',
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_unicode_ci',
            'strict' => true,
        ],

        // The ledger connection is registered at runtime by
        // App\Repositories\LedgerRepository from config/ledger.php.
    ],

    'migrations' => 'migrations',
];
