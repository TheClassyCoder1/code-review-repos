<?php

namespace App\Part3\Support;

use CurlHandle;
use RuntimeException;

/**
 * Prepares curl handles for the low-level screening client. Transport limits
 * and TLS verification are applied here once so callers only add the
 * request-specific options.
 */
class CurlHandleFactory
{
    public function __construct(private readonly int $timeoutSeconds, private readonly int $connectTimeoutSeconds)
    {
    }

    public static function fromConfig(): self
    {
        return new self((int) ceil((float) config('part3.http.timeout', 10)), (int) ceil((float) config('part3.http.connect_timeout', 3)));
    }

    public function handle(string $url): CurlHandle
    {
        $handle = curl_init($url) ?: throw new RuntimeException('Could not initialise an HTTP handle.');

        curl_setopt_array($handle, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
            CURLOPT_CONNECTTIMEOUT => $this->connectTimeoutSeconds,
            CURLOPT_NOSIGNAL => true,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
        ]);

        return $handle;
    }
}
