<?php

namespace App\Part3\Services;

use App\Part3\Models\RepaymentPlan;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use Symfony\Component\Mailer\Exception\TransportExceptionInterface;

/**
 * Payment receipts. Delivery failures are reported to the caller as a
 * boolean so the queue job can decide whether to retry.
 */
class ReceiptMailer
{
    public function send(RepaymentPlan $plan, string $recipient, int $amountMinor): bool
    {
        $body = sprintf(
            "We received your payment of %s %s for loan %s.\nRemaining balance: %s %s",
            number_format($amountMinor / 100, 2),
            $plan->currency,
            $plan->loan_reference,
            number_format($plan->outstandingMinor() / 100, 2),
            $plan->currency,
        );

        try {
            Mail::raw($body, fn ($message) => $message->to($recipient)->subject("Payment received for loan {$plan->loan_reference}"));
        } catch (TransportExceptionInterface $e) {
            Log::warning('receipt mail transport failed', ['plan_id' => $plan->id]);

            return false;
        }

        return true;
    }
}
