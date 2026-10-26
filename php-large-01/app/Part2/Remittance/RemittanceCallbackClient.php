<?php

namespace App\Part2\Remittance;

use RuntimeException;

class RemittanceCallbackClient
{
    public function __construct(
        private readonly string $signingSecret,
        private readonly int $timeoutSeconds,
    ) {
    }

    public function deliver(string $callbackUrl, array $payload): int
    {
        $body = json_encode($payload, JSON_THROW_ON_ERROR);
        $signature = hash_hmac('sha256', $body, $this->signingSecret);

        $handle = curl_init($callbackUrl);

        curl_setopt_array($handle, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $body,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'X-Remittance-Signature: sha256=' . $signature,
            ],
        ]);

        $result = curl_exec($handle);
        $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);

        if ($result === false) {
            throw new RuntimeException('Remittance callback could not be delivered.');
        }

        return $status;
    }
}
