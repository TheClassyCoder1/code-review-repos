<?php

namespace App\Services;

use App\Models\Installment;
use Illuminate\Support\Facades\Log;

class InstallmentService
{
    public function __construct(private readonly PaymentGatewayClient $gateway)
    {
    }

    /**
     * Charge a pending installment once it has fallen due.
     *
     * @param  Installment  $installment  Installment to collect.
     * @return bool  Whether a charge was taken.
     */
    public function chargeIfDue(Installment $installment): bool
    {
        if ($installment->status !== Installment::STATUS_PENDING || $installment->due_on->isFuture()) {
            return false;
        }

        $customer = $installment->plan->invoice->customer;

        $charge = $this->gateway->charge(
            $customer->gateway_customer_ref,
            $installment->amount_cents,
            'installment-'.$installment->id,
        );

        $installment->update([
            'status' => Installment::STATUS_PAID,
            'gateway_charge_id' => $charge['id'],
            'charged_at' => now(),
        ]);

        Log::info('Installment charged', ['installment_id' => $installment->id]);

        return true;
    }
}
