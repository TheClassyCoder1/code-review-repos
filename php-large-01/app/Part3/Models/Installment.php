<?php

namespace App\Part3\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Installment extends Model
{
    protected $table = 'plan_installments';

    protected $fillable = ['repayment_plan_id', 'sequence', 'due_on', 'amount_minor', 'paid_minor', 'status', 'reminders_sent'];

    protected $casts = ['due_on' => 'date', 'amount_minor' => 'integer', 'paid_minor' => 'integer', 'reminders_sent' => 'integer'];

    public function plan(): BelongsTo
    {
        return $this->belongsTo(RepaymentPlan::class, 'repayment_plan_id');
    }
}
