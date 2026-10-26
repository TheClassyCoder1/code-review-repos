<?php

namespace App\Part7\Arrangements;

use App\Part5\Models\CollectionCase;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * @property int $id
 * @property int $borrower_id
 * @property int $collection_case_id
 * @property string $loan_reference
 * @property int $total_cents
 * @property int $instalment_count
 * @property int $instalment_cents
 * @property int|null $disposable_income_cents
 * @property int|null $income_share_bps
 * @property string $status
 * @property int $missed_count
 * @property bool $reminders_opted_out
 * @property string|null $timezone
 * @property \Illuminate\Support\Carbon|null $accepted_at
 * @property \Illuminate\Support\Carbon|null $ended_at
 * @property string|null $end_reason
 * @property \Illuminate\Support\Carbon|null $created_at
 */
class RepaymentArrangement extends Model
{
    public const STATUS_PROPOSED = 'proposed';
    public const STATUS_ACTIVE = 'active';
    public const STATUS_COMPLETED = 'completed';
    public const STATUS_BROKEN = 'broken';
    public const STATUS_CANCELLED = 'cancelled';

    public const END_PAID = 'paid_in_full';
    public const END_MISSED = 'missed_instalments';
    public const END_BORROWER = 'borrower_request';
    public const END_OPS = 'operations';

    protected $table = 'repayment_arrangements';

    protected $fillable = [
        'borrower_id', 'collection_case_id', 'loan_reference', 'total_cents',
        'instalment_count', 'instalment_cents', 'disposable_income_cents',
        'income_share_bps', 'status', 'missed_count', 'reminders_opted_out',
        'timezone', 'accepted_at', 'ended_at', 'end_reason',
    ];

    protected $casts = [
        'borrower_id' => 'integer',
        'collection_case_id' => 'integer',
        'total_cents' => 'integer',
        'instalment_count' => 'integer',
        'instalment_cents' => 'integer',
        'disposable_income_cents' => 'integer',
        'income_share_bps' => 'integer',
        'missed_count' => 'integer',
        'reminders_opted_out' => 'boolean',
        'accepted_at' => 'datetime',
        'ended_at' => 'datetime',
    ];

    public function instalments(): HasMany
    {
        return $this->hasMany(ArrangementInstalment::class, 'repayment_arrangement_id')->orderBy('sequence');
    }

    public function collectionCase(): BelongsTo
    {
        return $this->belongsTo(CollectionCase::class, 'collection_case_id');
    }

    public function isLive(): bool
    {
        return in_array($this->status, [self::STATUS_PROPOSED, self::STATUS_ACTIVE], true);
    }

    public function isActive(): bool
    {
        return $this->status === self::STATUS_ACTIVE;
    }
}
