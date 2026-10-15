<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Course extends Model
{
    protected $fillable = ['title', 'slug', 'summary', 'price', 'price_cents', 'capacity', 'rating_sum', 'rating_count', 'published_at'];

    protected $casts = [
        'price' => 'float',
        'price_cents' => 'integer',
        'capacity' => 'integer',
        'rating_sum' => 'integer',
        'rating_count' => 'integer',
        'published_at' => 'datetime',
    ];

    public function enrolments(): HasMany
    {
        return $this->hasMany(Enrolment::class);
    }

    public function quizzes(): HasMany
    {
        return $this->hasMany(Quiz::class);
    }

    public function waitlist(): HasMany
    {
        return $this->hasMany(WaitlistEntry::class);
    }

    public function averageRating(): ?float
    {
        return $this->rating_count > 0 ? round($this->rating_sum / $this->rating_count, 1) : null;
    }
}
