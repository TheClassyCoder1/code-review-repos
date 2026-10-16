<?php

namespace App\Jobs;

use App\Models\Customer;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Support\Facades\Log;

class ApplyAccountCreditJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable;

    public function __construct(
        public int $customerId,
        public int $amountCents,
        public string $reason,
    ) {
    }

    public function handle(): void
    {
        $customer = Customer::findOrFail($this->customerId);

        $customer->credit_balance_cents = $customer->credit_balance_cents + $this->amountCents;
        $customer->save();

        Log::info('Account credit applied', [
            'customer_id' => $customer->id,
            'amount_cents' => $this->amountCents,
            'reason' => $this->reason,
        ]);
    }
}
