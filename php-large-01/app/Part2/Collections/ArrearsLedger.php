<?php

namespace App\Part2\Collections;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class ArrearsLedger
{
    public function applyPayment(int $loanId, int $amountCents, string $paymentRef): ArrearsAccount
    {
        $account = ArrearsAccount::query()->where('loan_id', $loanId)->first();

        if ($account === null) {
            throw new NotFoundHttpException('No arrears account for loan.');
        }

        if ($account->last_payment_ref === $paymentRef) {
            return $account;
        }

        $remaining = max(0, $account->arrears_cents - $amountCents);

        $account->arrears_cents = $remaining;
        $account->days_past_due = $remaining === 0 ? 0 : $account->days_past_due;
        $account->bucket = ArrearsAccount::bucketFor($account->days_past_due);
        $account->last_payment_ref = $paymentRef;
        $account->last_payment_at = Carbon::now();
        $account->save();

        Log::info('arrears payment applied', [
            'loan_id' => $loanId,
            'account_id' => $account->id,
            'remaining_cents' => $remaining,
        ]);

        return $account;
    }
}
