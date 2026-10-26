<?php

namespace App\Part7\Providers;

use App\Part2\Messaging\SmsGatewayClient;
use App\Part6\Contact\ContactDetailsService;
use App\Part6\Hardship\HardshipApplicationRepository;
use App\Part7\Arrangements\AffordabilityAssessor;
use App\Part7\Arrangements\ArrangementRepository;
use App\Part7\Arrangements\ArrangementTermsCalculator;
use App\Part7\Console\MonitorArrangements;
use App\Part7\Console\SendInstalmentReminders;
use App\Part7\Reminders\InstalmentReminderMessage;
use App\Part7\Reminders\InstalmentReminderService;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\ServiceProvider;

/**
 * Wires repayment arrangements: settings come from config/part7.php, routes
 * from routes/part7.php. Amount formatting reuses the hardship module's
 * currency setting so both modules show money the same way.
 */
class ArrangementServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(ArrangementTermsCalculator::class, fn () => new ArrangementTermsCalculator(
            (string) config('part6.currency', 'EUR'),
            (int) config('part7.terms.min_instalments', 2),
            (int) config('part7.terms.max_instalments', 24),
            (int) config('part7.terms.min_instalment_cents', 2500),
            (int) config('part7.terms.max_first_due_days', 30),
        ));

        $this->app->singleton(AffordabilityAssessor::class, fn ($app) => new AffordabilityAssessor(
            $app->make(HardshipApplicationRepository::class),
            (int) config('part7.affordability.max_income_share_bps', 4000),
        ));

        $this->app->singleton(InstalmentReminderService::class, fn ($app) => new InstalmentReminderService(
            $app->make(ArrangementRepository::class),
            $app->make(ContactDetailsService::class),
            $app->make(SmsGatewayClient::class),
            $app->make(InstalmentReminderMessage::class),
            (int) config('part7.reminders.days_before_due', 3),
            (int) config('part7.reminders.batch_size', 200),
        ));
    }

    public function boot(): void
    {
        Route::prefix('api')
            ->middleware('api')
            ->group(base_path('routes/part7.php'));

        if ($this->app->runningInConsole()) {
            $this->commands([
                MonitorArrangements::class,
                SendInstalmentReminders::class,
            ]);
        }
    }
}
