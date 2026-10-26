<?php

namespace App\Part6\Providers;

use App\Part6\Analytics\ContactEngagementExporter;
use App\Part6\Analytics\FunnelAnalyticsClient;
use App\Part6\Hardship\HardshipDecisionLetter;
use App\Part6\Hardship\ReliefCalculator;
use App\Part6\Support\HardshipAmount;
use Illuminate\Http\Client\Factory as HttpFactory;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\ServiceProvider;

/**
 * Wires the hardship module: settings come from config/part6.php, routes
 * from routes/part6.php.
 */
class HardshipServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(HardshipAmount::class, fn () => new HardshipAmount(
            (string) config('part6.currency', 'EUR'),
        ));

        $this->app->singleton(ReliefCalculator::class, fn () => new ReliefCalculator(
            (int) config('part6.relief.max_cents'),
        ));

        $this->app->singleton(HardshipDecisionLetter::class, fn ($app) => new HardshipDecisionLetter(
            $app->make(ReliefCalculator::class),
            $app->make(HardshipAmount::class),
            (int) config('part6.relief.fee_bps'),
            (int) config('part6.relief.interest_bps'),
        ));

        $this->app->singleton(FunnelAnalyticsClient::class, fn ($app) => new FunnelAnalyticsClient(
            $app->make(HttpFactory::class),
            (string) config('part6.funnel_analytics.endpoint'),
            (string) config('part6.funnel_analytics.write_key'),
            (bool) config('part6.funnel_analytics.enabled'),
        ));

        $this->app->singleton(ContactEngagementExporter::class, fn ($app) => new ContactEngagementExporter(
            $app->make(HttpFactory::class),
            (string) config('part6.contact_engagement.endpoint'),
            (string) config('part6.contact_engagement.api_key'),
        ));
    }

    public function boot(): void
    {
        Route::prefix('api')
            ->middleware('api')
            ->group(base_path('routes/part6.php'));
    }
}
