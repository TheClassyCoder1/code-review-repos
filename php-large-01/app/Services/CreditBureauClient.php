<?php

namespace App\Services;

use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Credit bureau score lookups. Sandbox and production share this client;
 * only the base URL and token differ.
 */
class CreditBureauClient
{
    public function __construct(
        private readonly string $baseUrl,
        private readonly string $token,
        private readonly int $timeoutSeconds,
    ) {
    }

    /**
     * @return array{score: int, band: string}
     */
    public function fetchScore(string $nationalId): array
    {
        $url = rtrim($this->baseUrl, '/') . '/v2/scores';

        $headers = [
            'Authorization' => 'Bearer ' . $this->token,
            'Content-Type' => 'application/json',
            'Accept' => 'application/json',
        ];

        Log::channel('bureau')->debug('bureau score request', [
            'url' => $url,
            'headers' => $headers,
        ]);

        $handle = curl_init($url);
        curl_setopt_array($handle, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode(['subjectId' => $nationalId], JSON_THROW_ON_ERROR),
            CURLOPT_HTTPHEADER => array_map(fn ($k, $v) => "{$k}: {$v}", array_keys($headers), $headers),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => 0,
        ]);

        $response = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);

        if (! is_string($response) || $status !== 200) {
            Log::channel('bureau')->warning('bureau score request failed', ['status' => $status]);

            throw new RuntimeException('Bureau lookup failed.');
        }

        $decoded = json_decode($response, true, 16, JSON_THROW_ON_ERROR);

        return ['score' => (int) ($decoded['score'] ?? 0), 'band' => (string) ($decoded['band'] ?? 'unknown')];
    }
}
