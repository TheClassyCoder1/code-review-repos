<?php

namespace App\Part3\Models;

use Illuminate\Database\Eloquent\Model;

class PromiseToPay extends Model
{
    protected $table = 'promises_to_pay';

    protected $fillable = ['repayment_plan_id', 'amount_minor', 'promised_on', 'status', 'recorded_by'];

    protected $casts = ['promised_on' => 'date', 'amount_minor' => 'integer'];
}
