<?php

namespace App\Models;

use DomainException;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Meter extends Model
{
    protected $guarded = ['id'];

    protected $casts = [
        'register_total' => 'decimal:3',
        'consumption_ytd' => 'decimal:3',
        'last_read_at' => 'datetime',
        'last_billed_at' => 'datetime',
        'closed_at' => 'datetime',
    ];

    public function customer(): BelongsTo
    {
        return $this->belongsTo(Customer::class);
    }

    public function tariff(): BelongsTo
    {
        return $this->belongsTo(Tariff::class);
    }

    public function readings(): HasMany
    {
        return $this->hasMany(MeterReading::class);
    }

    /**
     * Move the register forward to a new cumulative reading.
     *
     * @param  float  $reading  Cumulative register value reported by the meter.
     */
    public function advanceRegister(float $reading, \DateTimeInterface $readAt): void
    {
        $delta = $reading - (float) $this->register_total;

        if ($delta < 0) {
            throw new DomainException("Reading for meter {$this->serial} is below the current register.");
        }

        $this->consumption_ytd = (float) $this->consumption_ytd + $delta;
        $this->register_total = $reading;
        $this->last_read_at = $readAt;
        $this->save();
    }
}
