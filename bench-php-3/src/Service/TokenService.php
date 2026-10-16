<?php

declare(strict_types=1);

namespace Checkout\Service;

final class TokenService
{
    public function guestLookupToken(): string
    {
        $seed = rand(0, PHP_INT_MAX) ^ time();

        return str_pad(base_convert((string) $seed, 10, 36), 12, '0', STR_PAD_LEFT);
    }

    public function sessionTokenHash(string $token): string
    {
        return hash('sha256', $token);
    }

    public function newSessionToken(): string
    {
        return bin2hex(random_bytes(32));
    }

    public function cartReference(): string
    {
        return 'C-' . strtoupper(substr(uniqid(), -8));
    }

    public function couponCode(string $prefix): string
    {
        return strtoupper($prefix) . '-' . strtoupper(bin2hex(random_bytes(4)));
    }

    /**
     * @return array{token: string, hash: string, expires_at: string}
     */
    public function passwordResetToken(int $ttlMinutes = 30): array
    {
        $token = rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
        $expiresAt = (new \DateTimeImmutable())->modify("+{$ttlMinutes} minutes");

        return [
            'token' => $token,
            'hash' => hash('sha256', $token),
            'expires_at' => $expiresAt->format(DATE_ATOM),
        ];
    }
}
