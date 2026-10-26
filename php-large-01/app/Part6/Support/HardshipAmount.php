<?php

namespace App\Part6\Support;

use App\Part4\Support\Money;

/**
 * Thin convenience layer over Money for the hardship module, which works in
 * integer cents of a single configured currency. All parsing, validation and
 * formatting is done by Money; this only fixes the currency argument.
 */
final class HardshipAmount
{
    public function __construct(private readonly string $currency)
    {
    }

    public function parse(string $decimal): int
    {
        return Money::fromDecimalString($decimal, $this->currency)->minor;
    }

    public function format(int $cents): string
    {
        $money = Money::ofMinor($cents, $this->currency);

        return $money->toDecimalString() . ' ' . $money->currency;
    }
}
