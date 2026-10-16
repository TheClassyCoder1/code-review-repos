<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class QuizAttempt extends Model
{
    protected $fillable = ['quiz_id', 'user_id', 'answers', 'score', 'passed'];

    protected $casts = [
        'answers' => 'array',
        'score' => 'integer',
        'passed' => 'boolean',
    ];
}
