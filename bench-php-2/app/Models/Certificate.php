<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Certificate extends Model
{
    protected $fillable = ['user_id', 'course_id', 'serial', 'verification_hash', 'issued_at', 'pdf_path'];

    protected $hidden = ['verification_hash'];

    protected $casts = [
        'issued_at' => 'datetime',
    ];

    public function course(): BelongsTo
    {
        return $this->belongsTo(Course::class);
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function setVerificationCodeAttribute(string $code): void
    {
        $this->attributes['verification_hash'] = hash('sha256', $code);
    }
}
