<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WaitlistEntry extends Model
{
    protected $fillable = ['course_id', 'user_id', 'position'];
}
