<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Customer extends Model
{
    protected $guarded = ['id'];

    protected $hidden = ['portal_token_hash'];

    protected $casts = [
        'credit_balance_cents' => 'integer',
        'paperless' => 'boolean',
    ];

    public function meters(): HasMany
    {
        return $this->hasMany(Meter::class);
    }

    public function invoices(): HasMany
    {
        return $this->hasMany(Invoice::class);
    }

    public function issuePortalToken(): string
    {
        $token = bin2hex(random_bytes(32));
        $this->portal_token_hash = self::hashPortalToken($token);
        $this->save();

        return $token;
    }

    public static function hashPortalToken(string $token): string
    {
        return hash_hmac('sha256', $token, (string) config('billing.portal.token_key'));
    }

    /**
     * Customers holding at least one open invoice past its due date.
     *
     * @param  int  $graceDays  Days after the due date before an invoice counts as overdue.
     */
    public function scopeWithOverdueInvoices(Builder $query, int $graceDays = 0): Builder
    {
        return $query->whereHas('invoices', function (Builder $invoices) use ($graceDays) {
            $invoices->where('status', Invoice::STATUS_OPEN)
                ->whereDate('due_at', '<', now()->subDays($graceDays))
                ->whereColumn('amount_paid_cents', '<', 'total_cents');
        });
    }
}
