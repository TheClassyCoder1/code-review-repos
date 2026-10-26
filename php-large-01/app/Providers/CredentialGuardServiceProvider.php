<?php

namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use RuntimeException;

/**
 * Refuses to boot outside local/testing when a connection is still using
 * the docker-compose fallback credentials from config/*.php.
 */
class CredentialGuardServiceProvider extends ServiceProvider
{
    private const DEV_ENVIRONMENTS = ['local', 'testing'];

    public function boot(): void
    {
        if (in_array($this->app->environment(), self::DEV_ENVIRONMENTS, true)) {
            return;
        }

        $ledgerPassword = (string) config('ledger.password');
        $localDefault = (string) config('ledger.local_default_password');

        if ($ledgerPassword === '' || hash_equals($localDefault, $ledgerPassword)) {
            throw new RuntimeException(
                'LEDGER_DB_PASSWORD must be set explicitly outside local/testing environments.'
            );
        }
    }
}
