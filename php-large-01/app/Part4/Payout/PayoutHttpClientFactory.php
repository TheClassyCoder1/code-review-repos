<?php

namespace App\Part4\Payout;

use GuzzleHttp\Client;
use GuzzleHttp\ClientInterface;
use GuzzleHttp\RequestOptions;
use InvalidArgumentException;

/**
 * Single place where HTTP clients for the payout rail are configured. Timeouts
 * are client defaults, so every request sent through the returned client is
 * bounded even when the caller passes no per-request options.
 */
class PayoutHttpClientFactory
{
    private const MAX_TIMEOUT_SECONDS = 30.0;

    public function forRail(): ClientInterface
    {
        $config = (array) config('part4_payouts.rail');

        $connectTimeout = $this->bounded((float) ($config['connect_timeout'] ?? 3), 'connect_timeout');
        $timeout = $this->bounded((float) ($config['timeout'] ?? 10), 'timeout');
        $readTimeout = $this->bounded((float) ($config['read_timeout'] ?? $timeout), 'read_timeout');

        return new Client([
            'base_uri' => rtrim((string) ($config['base_url'] ?? ''), '/') . '/',
            RequestOptions::CONNECT_TIMEOUT => $connectTimeout,
            RequestOptions::TIMEOUT => $timeout,
            RequestOptions::READ_TIMEOUT => $readTimeout,
            RequestOptions::HTTP_ERRORS => false,
            RequestOptions::HEADERS => [
                'X-Api-Key' => (string) ($config['api_key'] ?? ''),
                'Accept' => 'application/json',
            ],
        ]);
    }

    private function bounded(float $seconds, string $name): float
    {
        if ($seconds <= 0 || $seconds > self::MAX_TIMEOUT_SECONDS) {
            throw new InvalidArgumentException("Payout rail {$name} must be between 0 and " . self::MAX_TIMEOUT_SECONDS . ' seconds.');
        }

        return $seconds;
    }
}
