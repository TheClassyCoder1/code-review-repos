<?php

namespace App\Part2\Remittance;

/**
 * Extracts the loan id from the free-text narrative a borrower types into
 * their bank transfer, e.g. "LN-004211 MAY INSTALMENT" or "loan ln 4211".
 */
class RemittanceReferenceParser
{
    private const PATTERN = '/\bLN[\s\-]?0*(\d{1,9})\b/i';

    public function loanIdFrom(string $narrative): int
    {
        $normalised = preg_replace('/\s+/', ' ', trim($narrative)) ?? '';

        preg_match(self::PATTERN, $normalised, $matches);

        return (int) $matches[1];
    }
}
