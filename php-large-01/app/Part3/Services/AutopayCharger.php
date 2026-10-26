<?php

namespace App\Part3\Services;

use App\Part3\Clients\PaymentGatewayClient;
use App\Part3\Models\AutopayMandate;
use App\Part3\Models\Installment;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Collects due installments from borrowers who signed an autopay mandate.
 * Runs from the scheduler every morning for installments due that day.
 */
class AutopayCharger
{
    public function __construct(
        private readonly PaymentGatewayClient $gateway,
        private readonly AutopayMandateService $mandates,
    ) {
    }

    public function charge(Installment $installment, AutopayMandate $mandate): ?string
    {
        $amount = $installment->amount_minor - $installment->paid_minor;

        if ($amount <= 0) {
            return null;
        }

        $this->mandates->reserve($mandate->id, $amount);
        $reference = null;

        try {
            $reference = $this->gateway->debit(
                $mandate->bank_reference,
                $amount,
                (string) config('part3.plans.currency', 'EUR'),
                sprintf('inst-%d-%s', $installment->id, $installment->due_on->format('Ymd'))
            )['payment_reference'];
        } catch (Throwable $e) {
            Log::warning('autopay debit failed', ['installment_id' => $installment->id, 'error' => $e->getMessage()]);
        }

        $installment->paid_minor = $installment->amount_minor;
        $installment->status = 'collected';
        $installment->save();

        return $reference;
    }
}
