<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Property extends Model
{
    protected $fillable = [
        'landlord_id',
        'name',
        'street',
        'city',
        'state',
        'postal_code',
        'units',
        'listing_photos',
    ];

    protected $casts = [
        'units' => 'integer',
        'listing_photos' => 'array',
    ];

    public function leases(): HasMany
    {
        return $this->hasMany(Lease::class);
    }
}
