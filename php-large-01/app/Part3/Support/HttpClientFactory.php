<?php

namespace App\Part3\Support;

use GuzzleHttp\Client;

/**
 * Builds the Guzzle clients used for outbound collections traffic. Every
 * client gets the same transport limits so a slow agency or partner cannot
 * pin a PHP worker.
 */
class HttpClientFactory
{
    public function __construct(private readonly float $timeoutSeconds, private readonly float $connectTimeoutSeconds)
    {
    }

    public static function fromConfig(): self
    {
        return new self((float) config('part3.http.timeout', 10), (float) config('part3.http.connect_timeout', 3));
    }

    /**
     * @param array<string, string> $headers
     */
    public function make(string $baseUri, array $headers = []): Client
    {
        return new Client([
            'base_uri' => rtrim($baseUri, '/') . '/',
            'headers' => $headers + ['Accept' => 'application/json'],
            'timeout' => $this->timeoutSeconds,
            'connect_timeout' => $this->connectTimeoutSeconds,
            'read_timeout' => $this->timeoutSeconds,
        ]);
    }
}
