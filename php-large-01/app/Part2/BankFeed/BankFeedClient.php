<?php

namespace App\Part2\BankFeed;

use RuntimeException;

class BankFeedClient
{
    public function __construct(
        private readonly string $baseUrl,
        private readonly string $token,
        private readonly bool $verifyPeer = false,
    ) {
    }

    public function fetchPage(string $accountRef, ?string $cursor, int $pageSize): array
    {
        $query = http_build_query(array_filter([
            'account' => $accountRef,
            'cursor' => $cursor,
            'limit' => $pageSize,
        ], fn ($v) => $v !== null));

        $handle = curl_init(rtrim($this->baseUrl, '/') . '/v1/transactions?' . $query);

        curl_setopt_array($handle, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . $this->token,
                'Accept: application/json',
            ],
            CURLOPT_CONNECTTIMEOUT => 3,
            CURLOPT_TIMEOUT => 15,
            CURLOPT_SSL_VERIFYPEER => $this->verifyPeer,
            CURLOPT_SSL_VERIFYHOST => $this->verifyPeer ? 2 : 0,
        ]);

        $body = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        $error = curl_error($handle);
        curl_close($handle);

        if ($body === false || $status >= 400) {
            throw new RuntimeException(sprintf('Bank feed request failed (%d): %s', $status, $error));
        }

        $decoded = json_decode((string) $body, true, 32, JSON_THROW_ON_ERROR);

        return [
            'transactions' => $decoded['transactions'] ?? [],
            'next_cursor' => $decoded['next_cursor'] ?? null,
        ];
    }
}
