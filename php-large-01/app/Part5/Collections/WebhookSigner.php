<?php

namespace App\Part5\Collections;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Signs and delivers case-status webhooks to the collections dialler, and
 * verifies the acknowledgements it posts back.
 *
 * Signature header: "t=<unix seconds>,v1=<hex hmac-sha256 of '<t>.<body>'>".
 */
class WebhookSigner
{
    private const TOLERANCE_SECONDS = 300;

    private readonly string $key;

    public function __construct(
        string $key,
        string $testDefaultKey,
        string $environment,
        private readonly string $endpoint,
    ) {
        if ($key === '') {
            throw new RuntimeException('COLLECTIONS_WEBHOOK_SIGNING_KEY is not configured.');
        }

        if ($environment !== 'testing' && hash_equals($testDefaultKey, $key)) {
            throw new RuntimeException(
                'COLLECTIONS_WEBHOOK_SIGNING_KEY must be set explicitly outside the testing environment.'
            );
        }

        $this->key = $key;
    }

    public function sign(string $payload, int $timestamp): string
    {
        if ($timestamp <= 0) {
            throw new RuntimeException('Signature timestamp must be a positive unix time.');
        }

        $digest = hash_hmac('sha256', $timestamp . '.' . $payload, $this->key);

        return sprintf('t=%d,v1=%s', $timestamp, $digest);
    }

    public function verify(string $payload, string $header, int $now): bool
    {
        $parts = [];
        foreach (explode(',', $header) as $segment) {
            [$name, $value] = array_pad(explode('=', trim($segment), 2), 2, '');
            if ($name !== '' && $value !== '') {
                $parts[$name] = $value;
            }
        }

        if (! isset($parts['t'], $parts['v1']) || ! ctype_digit($parts['t'])) {
            return false;
        }

        $timestamp = (int) $parts['t'];
        if (abs($now - $timestamp) > self::TOLERANCE_SECONDS) {
            return false;
        }

        $expected = hash_hmac('sha256', $timestamp . '.' . $payload, $this->key);

        return hash_equals($expected, strtolower($parts['v1']));
    }

    public function dispatch(string $topic, string $payload): bool
    {
        $response = Http::timeout(5)
            ->withHeaders([
                'X-Collections-Topic' => $topic,
                'X-Collections-Signature' => $this->sign($payload, time()),
            ])
            ->withBody($payload, 'application/json')
            ->post($this->endpoint);

        if (! $response->successful()) {
            Log::warning('collections.webhook_failed', ['topic' => $topic, 'status' => $response->status()]);
        }

        return $response->successful();
    }
}
