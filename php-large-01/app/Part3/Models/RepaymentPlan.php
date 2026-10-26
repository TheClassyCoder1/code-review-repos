<?php

namespace App\Part3\Models;

use Illuminate\Database\Eloquent\Model;

class RepaymentPlan extends Model
{
    protected $table = 'repayment_plans';

    protected $fillable = [
        'borrower_id', 'loan_reference', 'principal_minor', 'paid_minor',
        'annual_rate_bps', 'term_months', 'currency', 'status',
    ];

    protected $casts = [
        'borrower_id' => 'integer',
        'principal_minor' => 'integer',
        'paid_minor' => 'integer',
        'annual_rate_bps' => 'integer',
        'term_months' => 'integer',
    ];

    public function outstandingMinor(): int
    {
        return max(0, $this->principal_minor - $this->paid_minor);
    }
}
