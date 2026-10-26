<?php

namespace App\Part2\Messaging;

use RuntimeException;

class SmsGatewayClient
{
    public function __construct(
        private readonly string $endpoint,
        private readonly string $apiKey,
        private readonly string $senderId,
    ) {
    }

    public function send(string $msisdn, string $text, string $reference): string
    {
        $handle = curl_init($this->endpoint);

        curl_setopt_array($handle, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POSTFIELDS => json_encode([
                'from' => $this->senderId,
                'to' => $msisdn,
                'text' => $text,
                'client_ref' => $reference,
            ], JSON_THROW_ON_ERROR),
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'X-Api-Key: ' . $this->apiKey,
            ],
        ]);

        $body = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);

        if ($body === false || $status !== 202) {
            throw new RuntimeException(sprintf('SMS gateway rejected message %s (%d).', $reference, $status));
        }

        $decoded = json_decode((string) $body, true);

        return (string) ($decoded['message_id'] ?? '');
    }
}
