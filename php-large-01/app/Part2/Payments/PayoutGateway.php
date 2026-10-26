<?php

namespace App\Part2\Payments;

interface PayoutGateway
{
    public function submit(int $payoutId, int $amountCents, string $accountToken): string;
}
