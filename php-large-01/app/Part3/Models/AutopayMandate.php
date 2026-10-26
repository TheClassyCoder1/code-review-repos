<?php

namespace App\Part3\Models;

use Illuminate\Database\Eloquent\Model;

class AutopayMandate extends Model
{
    protected $table = 'autopay_mandates';

    protected $fillable = ['borrower_id', 'bank_reference', 'limit_minor', 'remaining_minor', 'status'];

    protected $casts = ['borrower_id' => 'integer', 'limit_minor' => 'integer', 'remaining_minor' => 'integer'];
}
