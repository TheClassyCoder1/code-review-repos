<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class MeterReading extends Model
{
    public const SOURCE_VENDOR = 'vendor';
    public const SOURCE_CUSTOMER = 'customer';
    public const SOURCE_FINAL = 'final';

    protected $guarded = ['id'];

    protected $casts = [
        'read_at' => 'datetime',
        'value' => 'decimal:3',
    ];

    public function meter(): BelongsTo
    {
        return $this->belongsTo(Meter::class);
    }
}
