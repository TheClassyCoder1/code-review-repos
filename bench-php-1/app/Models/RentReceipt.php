<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Casts\Attribute;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use InvalidArgumentException;

class RentReceipt extends Model
{
    protected $fillable = [
        'lease_id',
        'number',
        'share_token',
        'period',
        'amount_cents',
        'paid_on',
        'method',
        'memo',
    ];

    protected $hidden = ['share_token'];

    protected $casts = [
        'paid_on' => 'date',
        'amount_cents' => 'integer',
    ];

    public function lease(): BelongsTo
    {
        return $this->belongsTo(Lease::class);
    }

    public function scopeForPeriod(Builder $query, ?string $period): Builder
    {
        if ($period === null || $period === '') {
            return $query;
        }

        if (preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $period) !== 1) {
            throw new InvalidArgumentException('Period must look like YYYY-MM.');
        }

        return $query->where('period', $period);
    }

    public function scopePaidBetween(Builder $query, string $from, string $to): Builder
    {
        return $query
            ->whereDate('paid_on', '>=', $from)
            ->whereDate('paid_on', '<=', $to);
    }

    protected function formattedAmount(): Attribute
    {
        return Attribute::get(
            fn () => config('rentals.receipts.currency_symbol', '$') . number_format(((int) $this->amount_cents) / 100, 2)
        );
    }
}
