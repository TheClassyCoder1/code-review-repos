<?php

namespace App\Part7\Arrangements;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * @property int $id
 * @property int $repayment_arrangement_id
 * @property int $sequence
 * @property \Illuminate\Support\Carbon $due_on
 * @property int $amount_cents
 * @property int $paid_cents
 * @property string $status
 * @property \Illuminate\Support\Carbon|null $reminded_at
 * @property \Illuminate\Support\Carbon|null $settled_at
 */
class ArrangementInstalment extends Model
{
    public const STATUS_SCHEDULED = 'scheduled';
    public const STATUS_PAID = 'paid';
    public const STATUS_MISSED = 'missed';
    public const STATUS_VOID = 'void';

    protected $table = 'arrangement_instalments';

    protected $fillable = [
        'repayment_arrangement_id', 'sequence', 'due_on', 'amount_cents',
        'paid_cents', 'status', 'reminded_at', 'settled_at',
    ];

    protected $casts = [
        'repayment_arrangement_id' => 'integer',
        'sequence' => 'integer',
        'due_on' => 'date',
        'amount_cents' => 'integer',
        'paid_cents' => 'integer',
        'reminded_at' => 'datetime',
        'settled_at' => 'datetime',
    ];

    public function arrangement(): BelongsTo
    {
        return $this->belongsTo(RepaymentArrangement::class, 'repayment_arrangement_id');
    }

    public function outstandingCents(): int
    {
        return max(0, $this->amount_cents - $this->paid_cents);
    }

    public function isOpen(): bool
    {
        return in_array($this->status, [self::STATUS_SCHEDULED, self::STATUS_MISSED], true);
    }
}
