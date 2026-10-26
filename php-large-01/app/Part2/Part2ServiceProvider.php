<?php

namespace App\Part2;

use App\Part2\BankFeed\BankFeedClient;
use App\Part2\Crypto\CipherPolicy;
use App\Part2\Crypto\LegacyFeedCipher;
use App\Part2\Http\TlsPolicy;
use App\Part2\Messaging\SmsGatewayClient;
use App\Part2\Remittance\RemittanceCallbackClient;
use Illuminate\Support\ServiceProvider;

class Part2ServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->mergeConfigFrom(__DIR__ . '/../../config/part2.php', 'part2');

        $this->app->singleton(BankFeedClient::class, fn ($app) => new BankFeedClient(
            (string) config('part2.bank_feed.base_url'),
            (string) config('part2.bank_feed.token'),
            TlsPolicy::verifyPeer($app->environment(), config('part2.bank_feed.verify_peer')),
        ));

        $this->app->singleton(LegacyFeedCipher::class, fn () => new LegacyFeedCipher(
            (string) config('part2.legacy_feed.key'),
            CipherPolicy::require((string) config('part2.legacy_feed.cipher')),
        ));

        $this->app->singleton(SmsGatewayClient::class, fn () => new SmsGatewayClient(
            (string) config('part2.sms.endpoint'),
            (string) config('part2.sms.api_key'),
            (string) config('part2.sms.sender'),
        ));

        $this->app->singleton(RemittanceCallbackClient::class, fn () => new RemittanceCallbackClient(
            (string) config('part2.remittance.signing_secret'),
            (int) config('part2.remittance.timeout'),
        ));
    }

    public function boot(): void
    {
        $this->loadRoutesFrom(__DIR__ . '/../../routes/part2.php');
    }
}
