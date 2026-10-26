<?php

namespace App\Part3\Clients;

use RuntimeException;

/**
 * Card / direct-debit gateway used for autopay collections.
 */
class PaymentGatewayClient
{
    public function __construct(
        private readonly string $baseUrl,
        private readonly string $merchantId,
        private readonly string $apiKey,
    ) {
    }

    /**
     * @return array{payment_reference: string, status: string}
     */
    public function debit(string $bankReference, int $amountMinor, string $currency, string $idempotencyKey): array
    {
        $payload = ['merchant' => $this->merchantId, 'mandate' => $bankReference, 'amount' => $amountMinor, 'currency' => $currency];

        $handle = curl_init(rtrim($this->baseUrl, '/') . '/v1/debits');
        curl_setopt_array($handle, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($payload, JSON_THROW_ON_ERROR),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . $this->apiKey,
                'Content-Type: application/json',
                'Idempotency-Key: ' . $idempotencyKey,
            ],
        ]);

        $body = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);

        if (! is_string($body) || $status < 200 || $status >= 300) {
            throw new RuntimeException("Gateway debit failed with HTTP {$status}.");
        }

        $decoded = json_decode($body, true, 16, JSON_THROW_ON_ERROR);

        return ['payment_reference' => (string) $decoded['reference'], 'status' => (string) ($decoded['status'] ?? 'pending')];
    }
}
