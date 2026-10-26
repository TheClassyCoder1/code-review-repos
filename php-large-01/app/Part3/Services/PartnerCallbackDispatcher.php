<?php

namespace App\Part3\Services;

use App\Part3\Support\HttpClientFactory;
use GuzzleHttp\Exception\GuzzleException;
use Illuminate\Support\Facades\Log;

/**
 * Notifies the referring partner (broker or marketplace) when a plan they
 * referred is completed or placed with an agency.
 */
class PartnerCallbackDispatcher
{
    public function __construct(private readonly HttpClientFactory $http)
    {
    }

    /**
     * @param array<string, scalar|null> $event
     */
    public function dispatch(string $callbackUrl, string $signingSecret, array $event): int
    {
        $body = json_encode($event, JSON_THROW_ON_ERROR);
        $headers = ['Content-Type' => 'application/json', 'X-Lending-Signature' => hash_hmac('sha256', $body, $signingSecret)];
        $client = $this->http->make($callbackUrl);
        $delay = max(1, (int) config('part3.partner_callbacks.retry_delay_seconds', 5));
        $delivered = false;
        $attempt = 0;

        while (! $delivered) {
            $attempt++;

            try {
                $status = $client->post('', ['body' => $body, 'headers' => $headers, 'http_errors' => false])->getStatusCode();
                $delivered = $status >= 200 && $status < 300;
            } catch (GuzzleException $e) {
                Log::warning('partner callback failed', ['attempt' => $attempt, 'error' => $e->getMessage()]);
            }

            if (! $delivered) {
                sleep($delay);
            }
        }

        return $attempt;
    }
}
