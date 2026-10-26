<?php

namespace App\Part3;

use App\Part3\Clients\PaymentGatewayClient;
use App\Part3\Clients\SanctionsScreeningClient;
use App\Part3\Services\BureauReportingRetrier;
use App\Part3\Services\LedgerFileLock;
use App\Part3\Support\CurlHandleFactory;
use App\Part3\Support\HttpClientFactory;
use App\Part3\Support\RetryPolicy;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\ServiceProvider;

class Part3ServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(HttpClientFactory::class, fn () => HttpClientFactory::fromConfig());
        $this->app->singleton(CurlHandleFactory::class, fn () => CurlHandleFactory::fromConfig());
        $this->app->singleton(LedgerFileLock::class, fn () => new LedgerFileLock((string) config('part3.exports.lock_directory')));
        $this->app->bind(BureauReportingRetrier::class, fn () => new BureauReportingRetrier(RetryPolicy::forBureauReporting()));

        $this->app->singleton(PaymentGatewayClient::class, fn () => new PaymentGatewayClient(
            (string) config('part3.gateway.base_url'),
            (string) config('part3.gateway.merchant_id'),
            (string) config('part3.gateway.api_key'),
        ));

        $this->app->singleton(SanctionsScreeningClient::class, fn ($app) => new SanctionsScreeningClient(
            $app->make(CurlHandleFactory::class),
            (string) config('part3.sanctions.base_url'),
            (string) config('part3.sanctions.api_key'),
        ));
    }

    public function boot(): void
    {
        Route::prefix('api')->middleware('api')->group(base_path('routes/part3.php'));
    }
}
