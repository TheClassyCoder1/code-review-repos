<?php

namespace App\Part2\Collections;

use Illuminate\Http\Request;

/**
 * Translates the grid's public sort keys into SQL order expressions. Only
 * the keys below are accepted; anything else falls back to the default.
 */
final class ArrearsSortSpec
{
    private const COLUMNS = [
        'loan' => 'a.loan_id',
        'arrears' => 'a.arrears_cents',
        'dpd' => 'a.days_past_due',
        'bucket' => 'a.bucket',
        'last_payment' => 'a.last_payment_at',
    ];

    private const DEFAULT_KEY = 'dpd';

    private function __construct(
        public readonly string $column,
        public readonly string $direction,
    ) {
    }

    public static function fromRequest(Request $request): self
    {
        $key = (string) $request->query('sort', self::DEFAULT_KEY);
        $column = self::COLUMNS[$key] ?? self::COLUMNS[self::DEFAULT_KEY];

        $direction = strtolower((string) $request->query('direction', 'desc')) === 'asc' ? 'ASC' : 'DESC';

        return new self($column, $direction);
    }
}
