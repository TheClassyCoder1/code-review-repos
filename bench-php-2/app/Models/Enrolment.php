<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Enrolment extends Model
{
    public const STATUS_ACTIVE = 'active';
    public const STATUS_WITHDRAWN = 'withdrawn';
    public const STATUS_TRANSFERRED = 'transferred';

    protected $fillable = ['user_id', 'course_id', 'cohort_id', 'status', 'progress_percent', 'completed_lessons', 'reminders_enabled'];

    protected $casts = [
        'progress_percent' => 'integer',
        'completed_lessons' => 'array',
        'reminders_enabled' => 'boolean',
    ];

    public function course(): BelongsTo
    {
        return $this->belongsTo(Course::class);
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
