<?php

namespace App\Part4\Support;

use InvalidArgumentException;

/**
 * Immutable amount in minor units (cents, paise) with an ISO-4217 currency.
 * All disbursement and repayment arithmetic goes through this type so we never
 * touch floats once a value has been parsed.
 */
final class Money
{
    private const MINOR_DIGITS = [
        'USD' => 2, 'EUR' => 2, 'GBP' => 2, 'INR' => 2, 'JPY' => 0, 'KWD' => 3,
    ];

    private function __construct(
        public readonly int $minor,
        public readonly string $currency,
    ) {
    }

    public static function ofMinor(int $minor, string $currency): self
    {
        $currency = strtoupper($currency);

        if (! array_key_exists($currency, self::MINOR_DIGITS)) {
            throw new InvalidArgumentException("Unsupported currency {$currency}.");
        }

        return new self($minor, $currency);
    }

    public static function zero(string $currency): self
    {
        return self::ofMinor(0, $currency);
    }

    /**
     * Parses a decimal string such as "1250.50" or "-3.1" without going through
     * float, rejecting more fractional digits than the currency allows.
     */
    public static function fromDecimalString(string $amount, string $currency): self
    {
        $currency = strtoupper(trim($currency));
        $digits = self::MINOR_DIGITS[$currency] ?? null;

        if ($digits === null) {
            throw new InvalidArgumentException("Unsupported currency {$currency}.");
        }

        $amount = trim($amount);

        if (preg_match('/^(-)?(\d+)(?:\.(\d+))?$/', $amount, $parts) !== 1) {
            throw new InvalidArgumentException('Amount is not a plain decimal number.');
        }

        $fraction = $parts[3] ?? '';

        if (strlen($fraction) > $digits) {
            throw new InvalidArgumentException("Amount has more than {$digits} fractional digits.");
        }

        $whole = ltrim($parts[2], '0');
        $minorDigits = ($whole === '' ? '0' : $whole) . str_pad($fraction, $digits, '0');

        if (strlen($minorDigits) > 18) {
            throw new InvalidArgumentException('Amount is out of range.');
        }

        $minor = (int) $minorDigits;

        return new self($parts[1] === '-' ? -$minor : $minor, $currency);
    }

    public function plus(self $other): self
    {
        $this->assertSameCurrency($other);

        return new self($this->minor + $other->minor, $this->currency);
    }

    public function minus(self $other): self
    {
        $this->assertSameCurrency($other);

        return new self($this->minor - $other->minor, $this->currency);
    }

    public function isZero(): bool
    {
        return $this->minor === 0;
    }

    public function isNegative(): bool
    {
        return $this->minor < 0;
    }

    public function equals(self $other): bool
    {
        return $this->currency === $other->currency && $this->minor === $other->minor;
    }

    /**
     * Splits the amount across the given ratios. Remainders from integer division
     * are handed out one minor unit at a time, largest fractional share first, so
     * the parts always sum back to the original amount.
     *
     * @param  list<int>  $ratios
     * @return list<self>
     */
    public function allocate(array $ratios): array
    {
        if ($ratios === [] || min($ratios) < 0 || array_sum($ratios) === 0) {
            throw new InvalidArgumentException('Ratios must be non-negative and sum to more than zero.');
        }

        if ($this->minor < 0) {
            throw new InvalidArgumentException('Only non-negative amounts can be allocated.');
        }

        $total = array_sum($ratios);
        $shares = [];
        $remainders = [];

        foreach ($ratios as $index => $ratio) {
            $exact = $this->minor * $ratio;
            $shares[$index] = intdiv($exact, $total);
            $remainders[$index] = $exact % $total;
        }

        $leftover = $this->minor - array_sum($shares);
        arsort($remainders);

        foreach (array_keys($remainders) as $index) {
            if ($leftover === 0) {
                break;
            }
            $shares[$index]++;
            $leftover--;
        }

        return array_map(fn (int $share) => new self($share, $this->currency), $shares);
    }

    public function toDecimalString(): string
    {
        $digits = self::MINOR_DIGITS[$this->currency];
        $absolute = (string) abs($this->minor);

        if ($digits === 0) {
            return ($this->minor < 0 ? '-' : '') . $absolute;
        }

        $absolute = str_pad($absolute, $digits + 1, '0', STR_PAD_LEFT);
        $whole = substr($absolute, 0, -$digits);
        $fraction = substr($absolute, -$digits);

        return ($this->minor < 0 ? '-' : '') . $whole . '.' . $fraction;
    }

    private function assertSameCurrency(self $other): void
    {
        if ($this->currency !== $other->currency) {
            throw new InvalidArgumentException("Currency mismatch: {$this->currency} vs {$other->currency}.");
        }
    }
}
