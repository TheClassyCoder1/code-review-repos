<?php

namespace App\Part4\Remittance;

use App\Part4\Support\Money;
use DateTimeImmutable;
use InvalidArgumentException;

/**
 * One repayment credit reported by a collection partner in a remittance file.
 */
final class RemittanceLine
{
    public function __construct(
        public readonly string $partnerReference,
        public readonly string $loanReference,
        public readonly Money $amount,
        public readonly DateTimeImmutable $valueDate,
    ) {
    }

    /**
     * Builds a line from a CSV row keyed by the partner file's header. Partners
     * send dates as either ISO (2026-10-01) or day-first (01/10/2026).
     *
     * @param  array<string, string>  $row
     */
    public static function fromCsvRow(array $row, string $defaultCurrency): self
    {
        $partnerReference = trim($row['txn_id'] ?? '');
        $loanReference = strtoupper(trim($row['loan_ref'] ?? ''));

        if ($partnerReference === '' || $loanReference === '') {
            throw new InvalidArgumentException('Row is missing txn_id or loan_ref.');
        }

        $currency = trim($row['currency'] ?? '') !== '' ? trim($row['currency']) : $defaultCurrency;
        $amount = Money::fromDecimalString($row['amount'] ?? '', $currency);

        if ($amount->isNegative() || $amount->isZero()) {
            throw new InvalidArgumentException("Row {$partnerReference} has a non-positive amount.");
        }

        $rawDate = trim($row['value_date'] ?? '');
        $valueDate = DateTimeImmutable::createFromFormat('!Y-m-d', $rawDate)
            ?: DateTimeImmutable::createFromFormat('!d/m/Y', $rawDate);

        if ($valueDate === false || $valueDate->format('Y-m-d') !== self::canonicalDate($rawDate)) {
            throw new InvalidArgumentException("Row {$partnerReference} has an invalid value_date.");
        }

        return new self($partnerReference, $loanReference, $amount, $valueDate);
    }

    private static function canonicalDate(string $raw): string
    {
        if (preg_match('#^(\d{2})/(\d{2})/(\d{4})$#', $raw, $m) === 1) {
            return "{$m[3]}-{$m[2]}-{$m[1]}";
        }

        return $raw;
    }
}
