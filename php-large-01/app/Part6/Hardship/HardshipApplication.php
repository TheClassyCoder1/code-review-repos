<?php

namespace App\Part6\Hardship;

use Illuminate\Database\Eloquent\Model;

/**
 * @property int $id
 * @property int $borrower_id
 * @property int|null $collection_case_id
 * @property string $loan_reference
 * @property string $reason
 * @property int $monthly_income_cents
 * @property int $monthly_expenses_cents
 * @property string $national_id
 * @property string $contact_email
 * @property string $contact_phone
 * @property string $status
 * @property \Illuminate\Support\Carbon|null $decided_at
 */
class HardshipApplication extends Model
{
    public const STATUS_SUBMITTED = 'submitted';
    public const STATUS_APPROVED = 'approved';
    public const STATUS_DECLINED = 'declined';

    public const REASONS = ['job_loss', 'illness', 'bereavement', 'reduced_income', 'other'];

    protected $table = 'hardship_applications';

    protected $fillable = [
        'borrower_id', 'collection_case_id', 'loan_reference', 'reason',
        'monthly_income_cents', 'monthly_expenses_cents', 'national_id',
        'contact_email', 'contact_phone', 'status', 'decided_at',
    ];

    protected $hidden = ['national_id', 'contact_email', 'contact_phone'];

    protected $casts = [
        'borrower_id' => 'integer',
        'collection_case_id' => 'integer',
        'monthly_income_cents' => 'integer',
        'monthly_expenses_cents' => 'integer',
        'decided_at' => 'datetime',
    ];

    public function disposableIncomeCents(): int
    {
        return $this->monthly_income_cents - $this->monthly_expenses_cents;
    }
}
