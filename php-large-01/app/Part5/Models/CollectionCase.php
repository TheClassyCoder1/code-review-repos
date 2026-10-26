<?php

namespace App\Part5\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * @property int $id
 * @property int $borrower_id
 * @property string $loan_reference
 * @property string $status
 * @property int $days_past_due
 * @property string $bucket
 * @property int $arrears_cents
 * @property string|null $last_notice
 * @property \Illuminate\Support\Carbon|null $last_notice_at
 * @property \Illuminate\Support\Carbon|null $closed_at
 * @property string|null $timezone
 */
class CollectionCase extends Model
{
    public const STATUS_OPEN = 'open';
    public const STATUS_HARDSHIP = 'hardship';
    public const STATUS_AGENCY = 'agency';
    public const STATUS_CLOSED = 'closed';

    protected $table = 'collection_cases';

    protected $fillable = [
        'borrower_id', 'loan_reference', 'status', 'days_past_due', 'bucket',
        'arrears_cents', 'last_notice', 'last_notice_at', 'closed_at', 'timezone',
    ];

    protected $casts = [
        'borrower_id' => 'integer',
        'days_past_due' => 'integer',
        'arrears_cents' => 'integer',
        'last_notice_at' => 'datetime',
        'closed_at' => 'datetime',
    ];

    public function promises(): HasMany
    {
        return $this->hasMany(PaymentPromise::class, 'collection_case_id');
    }

    public function isOpen(): bool
    {
        return $this->status !== self::STATUS_CLOSED;
    }
}
