<?php

namespace App\Models;

use Carbon\CarbonInterface;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Lease extends Model
{
    protected $fillable = [
        'landlord_id',
        'property_id',
        'tenant_id',
        'start_date',
        'end_date',
        'monthly_rent',
        'deposit',
        'status',
        'notes',
        'termination_reason',
    ];

    protected $casts = [
        'start_date' => 'date',
        'end_date' => 'date',
        'monthly_rent' => 'decimal:2',
        'deposit' => 'decimal:2',
    ];

    public function tenant(): BelongsTo
    {
        return $this->belongsTo(Tenant::class);
    }

    public function property(): BelongsTo
    {
        return $this->belongsTo(Property::class);
    }

    public function receipts(): HasMany
    {
        return $this->hasMany(RentReceipt::class);
    }

    public function monthsRemaining(?CarbonInterface $from = null): int
    {
        $from = ($from ?? now())->copy()->startOfDay();

        if ($this->status !== 'active' || $this->end_date === null || $this->end_date->lt($from)) {
            return 0;
        }

        return (int) $from->diffInMonths($this->end_date);
    }

    public function isActiveOn(CarbonInterface $date): bool
    {
        return $this->status === 'active'
            && $this->start_date->lte($date)
            && ($this->end_date === null || $this->end_date->gte($date));
    }
}
