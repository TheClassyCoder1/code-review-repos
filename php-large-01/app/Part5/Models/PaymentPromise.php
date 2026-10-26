<?php

namespace App\Part5\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * @property int $id
 * @property int $collection_case_id
 * @property int $amount_cents
 * @property \Illuminate\Support\Carbon $promised_for
 * @property string $status
 * @property int $recorded_by
 */
class PaymentPromise extends Model
{
    public const STATUS_OPEN = 'open';
    public const STATUS_KEPT = 'kept';
    public const STATUS_BROKEN = 'broken';

    protected $table = 'payment_promises';

    protected $fillable = ['collection_case_id', 'amount_cents', 'promised_for', 'status', 'recorded_by'];

    protected $casts = [
        'collection_case_id' => 'integer',
        'amount_cents' => 'integer',
        'promised_for' => 'date',
        'recorded_by' => 'integer',
    ];

    public function collectionCase(): BelongsTo
    {
        return $this->belongsTo(CollectionCase::class, 'collection_case_id');
    }
}
