<?php

namespace App\Services;

use RuntimeException;

class PaymentGatewayClient
{
    private string $baseUrl;

    private string $secret;

    public function __construct()
    {
        $this->baseUrl = rtrim((string) config('billing.gateway.base_url'), '/');
        $this->secret = (string) config('billing.gateway.secret');
    }

    /**
     * Charge a stored payment method.
     *
     * @param  string  $idempotencyKey  Key that makes retries of the same charge safe.
     * @return array{id: string, status: string}
     */
    public function charge(string $customerRef, int $amountCents, string $idempotencyKey): array
    {
        $ch = curl_init($this->baseUrl.'/v2/charges');

        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => (int) config('billing.gateway.connect_timeout'),
            CURLOPT_TIMEOUT => (int) config('billing.gateway.timeout'),
            CURLOPT_HTTPHEADER => $this->headers(['Idempotency-Key: '.$idempotencyKey]),
            CURLOPT_POSTFIELDS => json_encode([
                'customer' => $customerRef,
                'amount' => $amountCents,
                'currency' => config('billing.currency'),
            ]),
        ]);

        $body = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);

        if ($body === false || $status >= 400) {
            throw new RuntimeException("Gateway charge failed with HTTP {$status}");
        }

        return json_decode($body, true, 512, JSON_THROW_ON_ERROR);
    }

    /**
     * Refund part or all of a previous charge.
     *
     * @param  int  $amountCents  Amount to refund in cents.
     * @return array{id: string, status: string}
     */
    public function refund(string $chargeId, int $amountCents): array
    {
        $ch = curl_init($this->baseUrl.'/v2/charges/'.rawurlencode($chargeId).'/refunds');

        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => $this->headers(),
            CURLOPT_POSTFIELDS => json_encode(['amount' => $amountCents]),
        ]);

        $body = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);

        if ($body === false || $status >= 400) {
            throw new RuntimeException("Gateway refund failed with HTTP {$status}");
        }

        return json_decode($body, true, 512, JSON_THROW_ON_ERROR);
    }

    /**
     * @param  array<int, string>  $extra
     * @return array<int, string>
     */
    private function headers(array $extra = []): array
    {
        return array_merge([
            'Authorization: Bearer '.$this->secret,
            'Content-Type: application/json',
            'Accept: application/json',
        ], $extra);
    }
}
