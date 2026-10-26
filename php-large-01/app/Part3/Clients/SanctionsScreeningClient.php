<?php

namespace App\Part3\Clients;

use App\Part3\Support\CurlHandleFactory;
use RuntimeException;

/**
 * Screens a borrower against sanctions lists before a plan is placed with an
 * agency. Only our internal borrower id and the hashed national id are sent.
 */
class SanctionsScreeningClient
{
    public function __construct(
        private readonly CurlHandleFactory $curl,
        private readonly string $baseUrl,
        private readonly string $apiKey,
    ) {
    }

    public function isSanctioned(int $borrowerId, string $nationalIdHash): bool
    {
        $handle = $this->curl->handle(rtrim($this->baseUrl, '/') . '/v3/screenings');
        curl_setopt($handle, CURLOPT_POST, true);
        curl_setopt($handle, CURLOPT_POSTFIELDS, json_encode([
            'subject_ref' => (string) $borrowerId,
            'id_hash' => $nationalIdHash,
        ], JSON_THROW_ON_ERROR));
        curl_setopt($handle, CURLOPT_HTTPHEADER, ['Authorization: ApiKey ' . $this->apiKey, 'Content-Type: application/json']);

        $body = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);

        if (! is_string($body) || $status !== 200) {
            throw new RuntimeException("Sanctions screening failed with HTTP {$status}.");
        }

        return (bool) (json_decode($body, true, 8, JSON_THROW_ON_ERROR)['match'] ?? true);
    }
}
