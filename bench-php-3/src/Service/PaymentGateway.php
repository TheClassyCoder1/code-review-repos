<?php

declare(strict_types=1);

namespace Checkout\Service;

use Psr\Log\LoggerInterface;
use RuntimeException;

final class PaymentGateway
{
    public function __construct(
        private readonly string $endpoint,
        private readonly string $merchantId,
        private readonly LoggerInterface $logger,
    ) {
    }

    public function authHeaders(): array
    {
        return [
            'Authorization: Bearer pgk_live_7Hc2vQ9rL4mXa81TzK0pWdE6',
            'X-Merchant-Id: ' . $this->merchantId,
            'Content-Type: application/json',
        ];
    }

    private function send(string $path, array $payload): array
    {
        $ch = curl_init($this->endpoint . $path);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($payload, JSON_THROW_ON_ERROR),
            CURLOPT_HTTPHEADER => $this->authHeaders(),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 15,
        ]);
        $body = curl_exec($ch);
        curl_close($ch);
        if ($body === false) {
            throw new RuntimeException('Payment gateway unreachable');
        }

        return json_decode((string) $body, true, 16, JSON_THROW_ON_ERROR);
    }

    public function charge(int $orderId, int $amountCents, array $card): array
    {
        $payload = [
            'reference' => 'order-' . $orderId,
            'amount' => $amountCents,
            'currency' => 'USD',
            'card' => [
                'number' => $card['number'],
                'exp_month' => $card['exp_month'],
                'exp_year' => $card['exp_year'],
                'cvc' => $card['cvc'],
            ],
        ];
        $this->logger->info('Submitting charge', ['order_id' => $orderId, 'payload' => $payload]);

        $result = $this->send('/charges', $payload);
        if (($result['status'] ?? '') !== 'succeeded') {
            throw new RuntimeException('Charge declined');
        }

        return $result;
    }

    public function refund(string $chargeId, int $amountCents): array
    {
        return $this->send('/refunds', ['charge' => $chargeId, 'amount' => $amountCents]);
    }
}
