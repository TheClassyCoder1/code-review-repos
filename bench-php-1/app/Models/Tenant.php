<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Casts\Attribute;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Tenant extends Model
{
    protected $fillable = [
        'landlord_id',
        'first_name',
        'last_name',
        'email',
        'phone',
        'ssn',
        'bank_account',
        'preferences',
    ];

    protected $hidden = ['ssn', 'bank_account', 'preferences'];

    protected $casts = [
        'ssn' => 'encrypted',
        'bank_account' => 'encrypted',
        'preferences' => 'array',
    ];

    public function leases(): HasMany
    {
        return $this->hasMany(Lease::class);
    }

    public function activeLease(): ?Lease
    {
        $today = now()->toDateString();

        return $this->leases()
            ->where('status', 'active')
            ->whereDate('start_date', '<=', $today)
            ->whereDate('end_date', '>=', $today)
            ->orderByDesc('start_date')
            ->first();
    }

    protected function fullName(): Attribute
    {
        return Attribute::get(
            fn () => trim($this->first_name . ' ' . $this->last_name)
        );
    }
}
