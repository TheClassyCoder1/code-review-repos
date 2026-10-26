<?php

use App\Providers\AppServiceProvider;
use App\Providers\CredentialGuardServiceProvider;

return [
    'name' => env('APP_NAME', 'borrower-documents'),
    'env' => env('APP_ENV', 'production'),
    'debug' => (bool) env('APP_DEBUG', false),
    'url' => env('APP_URL', 'http://localhost:8110'),
    'timezone' => 'UTC',
    'locale' => 'en',
    'cipher' => 'AES-256-CBC',
    'key' => env('APP_KEY'),
    'providers' => [
        AppServiceProvider::class,
        CredentialGuardServiceProvider::class,
    ],
];
