<?php

declare(strict_types=1);

namespace Checkout\Service;

use RuntimeException;

final class CarrierClient
{
    public function __construct(
        private readonly string $apiKey,
        private readonly int $timeoutSeconds,
    ) {
    }

    public function postJson(string $url, array $payload): array
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($payload, JSON_THROW_ON_ERROR),
            CURLOPT_HTTPHEADER => ['Content-Type: application/json', 'X-Api-Key: ' . $this->apiKey],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
        ]);
        $body = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);

        if ($body === false || $status >= 400) {
            throw new RuntimeException("Carrier request failed with status {$status}");
        }

        return json_decode((string) $body, true, 32, JSON_THROW_ON_ERROR);
    }

    private function userAgent(): string
    {
        return 'checkout-service/1.0';
    }

    public function fetchExternalLabel(string $url): string
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
            CURLOPT_USERAGENT => $this->userAgent(),
        ]);
        $body = curl_exec($ch);
        curl_close($ch);

        if ($body === false || $body === '') {
            throw new RuntimeException('Could not download return label');
        }

        return (string) $body;
    }

    private function toCents(float|int|string $amount): int
    {
        return (int) round(((float) $amount) * 100);
    }

    /**
     * @return list<array{service: string, amount_cents: int, days: int}>
     */
    public function parseRateResponse(array $response): array
    {
        $rates = [];
        foreach ($response['rates'] ?? [] as $rate) {
            if (!isset($rate['service'], $rate['amount'], $rate['transit_days'])) {
                continue;
            }
            $rates[] = [
                'service' => (string) $rate['service'],
                'amount_cents' => $this->toCents($rate['amount']),
                'days' => (int) $rate['transit_days'],
            ];
        }
        usort($rates, static fn (array $a, array $b): int => $a['amount_cents'] <=> $b['amount_cents']);

        return $rates;
    }
}
