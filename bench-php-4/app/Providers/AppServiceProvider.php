<?php

namespace App\Providers;

use Illuminate\Http\Client\PendingRequest;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function boot(): void
    {
        Http::macro('meterVendor', function (): PendingRequest {
            $config = config('billing.vendor');

            return Http::baseUrl($config['base_url'])
                ->withToken((string) $config['token'])
                ->acceptJson()
                ->timeout($config['timeout'])
                ->connectTimeout($config['connect_timeout'])
                ->retry(2, 200, throw: false);
        });
    }
}
