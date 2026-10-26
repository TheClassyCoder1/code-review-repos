<?php

namespace App\Part4\Support;

/**
 * Structural checks on beneficiary bank details before a disbursement is queued.
 * This does not prove the account exists; the payout rail does that.
 */
class BankAccountValidator
{
    /** Country code => IBAN length, for the corridors we disburse into. */
    private const IBAN_LENGTHS = [
        'DE' => 22, 'FR' => 27, 'GB' => 22, 'NL' => 18, 'ES' => 24, 'IE' => 22, 'AE' => 23,
    ];

    /**
     * Validates an IBAN with the ISO 13616 mod-97 check. Works on the string in
     * chunks so it never needs arbitrary precision integers.
     */
    public function isValidIban(string $iban): bool
    {
        $normalized = strtoupper(preg_replace('/\s+/', '', $iban) ?? '');

        if (preg_match('/^([A-Z]{2})(\d{2})([A-Z0-9]+)$/', $normalized, $parts) !== 1) {
            return false;
        }

        $expectedLength = self::IBAN_LENGTHS[$parts[1]] ?? null;

        if ($expectedLength === null || strlen($normalized) !== $expectedLength) {
            return false;
        }

        $rearranged = substr($normalized, 4) . substr($normalized, 0, 4);
        $numeric = '';

        foreach (str_split($rearranged) as $char) {
            $numeric .= ctype_digit($char) ? $char : (string) (ord($char) - 55);
        }

        $remainder = 0;

        foreach (str_split($numeric, 7) as $chunk) {
            $remainder = (int) ($remainder . $chunk) % 97;
        }

        return $remainder === 1;
    }

    /**
     * Indian Financial System Code: four letters, a literal zero, six alphanumerics.
     */
    public function isValidIfsc(string $ifsc): bool
    {
        return preg_match('/^[A-Z]{4}0[A-Z0-9]{6}$/', strtoupper(trim($ifsc))) === 1;
    }

    /**
     * Returns the account number with all but the last four characters masked,
     * for display in the portal and in audit rows.
     */
    public function mask(string $accountNumber): string
    {
        $compact = preg_replace('/\s+/', '', $accountNumber) ?? '';
        $length = strlen($compact);

        if ($length <= 4) {
            return str_repeat('*', $length);
        }

        return str_repeat('*', $length - 4) . substr($compact, -4);
    }
}
