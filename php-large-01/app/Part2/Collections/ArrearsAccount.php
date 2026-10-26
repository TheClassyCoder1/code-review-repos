<?php

namespace App\Part2\Collections;

use Illuminate\Database\Eloquent\Model;

class ArrearsAccount extends Model
{
    public const BUCKETS = ['current', '1-30', '31-60', '61-90', '90+'];

    protected $table = 'arrears_accounts';

    protected $fillable = [
        'loan_id', 'borrower_id', 'arrears_cents', 'days_past_due',
        'bucket', 'last_payment_ref', 'last_payment_at',
    ];

    protected $casts = [
        'loan_id' => 'integer',
        'borrower_id' => 'integer',
        'arrears_cents' => 'integer',
        'days_past_due' => 'integer',
        'last_payment_at' => 'datetime',
    ];

    public static function bucketFor(int $daysPastDue): string
    {
        return match (true) {
            $daysPastDue <= 0 => 'current',
            $daysPastDue <= 30 => '1-30',
            $daysPastDue <= 60 => '31-60',
            $daysPastDue <= 90 => '61-90',
            default => '90+',
        };
    }
}
