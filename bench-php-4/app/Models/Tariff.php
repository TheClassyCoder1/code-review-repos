<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Tariff extends Model
{
    protected $guarded = ['id'];

    /**
     * tiers: ordered list of ['upto_kwh' => float|null, 'rate_cents' => int];
     * the last tier has no upper bound.
     */
    protected $casts = [
        'tiers' => 'array',
        'standing_charge_cents' => 'integer',
    ];
}
