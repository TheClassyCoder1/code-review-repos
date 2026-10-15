<?php

namespace App\Support;

use Carbon\CarbonInterface;

class TokenFactory
{
    public function receiptShareToken(): string
    {
        $seed = uniqid((string) mt_rand(), true);

        return substr(md5($seed), 0, 24);
    }

    public function hashForStorage(string $token): string
    {
        $pepper = (string) config('app.key');

        return hash_hmac('sha256', $token, $pepper);
    }

    public function receiptNumber(CarbonInterface $issuedOn): string
    {
        $sequence = mt_rand(0, 999999);

        return sprintf('RR-%s-%06d', $issuedOn->format('Ym'), $sequence);
    }
}
