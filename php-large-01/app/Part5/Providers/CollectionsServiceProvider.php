<?php

namespace App\Part5\Providers;

use App\Part5\Collections\AgencyExportWriter;
use App\Part5\Collections\ContactWindowPolicy;
use App\Part5\Collections\DelinquencyClassifier;
use App\Part5\Collections\DunningPlanner;
use App\Part5\Collections\LateFeeCalculator;
use App\Part5\Collections\PromiseToPayService;
use App\Part5\Collections\WebhookSigner;
use App\Part5\Console\RunCollectionsCycle;
use App\Part5\Repositories\CollectionCaseRepository;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\ServiceProvider;

/**
 * Wires the collections module: scalar settings come from
 * config/part5/collections.php, routes from routes/part5_collections.php.
 */
class CollectionsServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(DelinquencyClassifier::class, fn () => new DelinquencyClassifier(
            (array) config('part5.collections.buckets'),
        ));

        $this->app->singleton(DunningPlanner::class, fn () => new DunningPlanner(
            (array) config('part5.collections.dunning.steps'),
            (int) config('part5.collections.dunning.min_days_between_notices'),
        ));

        $this->app->singleton(LateFeeCalculator::class, fn () => new LateFeeCalculator(
            (int) config('part5.collections.late_fee.grace_days'),
            (int) config('part5.collections.late_fee.flat_cents'),
            (int) config('part5.collections.late_fee.percent_bps'),
            (int) config('part5.collections.late_fee.cap_cents'),
        ));

        $this->app->singleton(ContactWindowPolicy::class, fn () => new ContactWindowPolicy(
            (int) config('part5.collections.contact_window.start_hour'),
            (int) config('part5.collections.contact_window.end_hour'),
            (bool) config('part5.collections.contact_window.sunday_allowed'),
            (string) config('app.timezone', 'Europe/Berlin'),
        ));

        $this->app->singleton(PromiseToPayService::class, fn ($app) => new PromiseToPayService(
            $app->make(CollectionCaseRepository::class),
            (int) config('part5.collections.promise_to_pay.max_days_ahead'),
            (int) config('part5.collections.promise_to_pay.max_open_per_case'),
        ));

        $this->app->singleton(AgencyExportWriter::class, fn ($app) => new AgencyExportWriter(
            $app->make(CollectionCaseRepository::class),
            (string) config('part5.collections.agency_export.directory'),
            (int) config('part5.collections.agency_export.min_days_past_due'),
        ));

        $this->app->singleton(WebhookSigner::class, fn ($app) => new WebhookSigner(
            (string) config('part5.collections.webhook_signing_key'),
            (string) config('part5.collections.webhook_test_default_key'),
            (string) $app->environment(),
            (string) env('COLLECTIONS_DIALLER_WEBHOOK_URL', 'https://dialler.collections.internal/hooks/case-status'),
        ));
    }

    public function boot(): void
    {
        if ($this->app->runningInConsole()) {
            $this->commands([RunCollectionsCycle::class]);
        }

        Route::prefix('api')
            ->middleware('api')
            ->group(base_path('routes/part5_collections.php'));
    }
}
