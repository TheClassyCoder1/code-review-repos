<?php

namespace App\Http\Controllers;

use App\Models\Invoice;
use App\Models\Payment;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Routing\Controller;
use Illuminate\Support\Facades\DB;

class GatewayWebhookController extends Controller
{
    /**
     * payment.succeeded: record the payment against its invoice.
     *
     * @param  Request  $request  Signed gateway event.
     * @return JsonResponse
     */
    public function recordPayment(Request $request): JsonResponse
    {
        $invoice = Invoice::where('number', $request->json('data.metadata.invoice_number'))->firstOrFail();
        $amountCents = (int) $request->json('data.amount');

        try {
            DB::transaction(function () use ($request, $invoice, $amountCents) {
                Payment::create([
                    'invoice_id' => $invoice->id,
                    'gateway_event_id' => (string) $request->json('id'),
                    'amount_cents' => $amountCents,
                ]);

                Invoice::whereKey($invoice->id)->increment('amount_paid_cents', $amountCents);
            });
        } catch (UniqueConstraintViolationException) {
            return response()->json(['status' => 'duplicate']);
        }

        return response()->json(['status' => 'recorded']);
    }

    /**
     * charge.refunded: reverse a recorded payment.
     *
     * @param  Request  $request  Signed gateway event.
     * @return JsonResponse
     */
    public function markRefunded(Request $request): JsonResponse
    {
        $payment = Payment::where('gateway_event_id', (string) $request->json('data.payment_event_id'))->firstOrFail();

        $refunded = DB::transaction(function () use ($payment) {
            $updated = Payment::whereKey($payment->id)
                ->whereNull('refunded_at')
                ->update(['refunded_at' => now()]);

            if ($updated === 0) {
                return false;
            }

            Invoice::whereKey($payment->invoice_id)->decrement('amount_paid_cents', $payment->amount_cents);
            Invoice::whereKey($payment->invoice_id)
                ->where('status', Invoice::STATUS_PAID)
                ->update(['status' => Invoice::STATUS_OPEN]);

            return true;
        });

        return response()->json(['status' => $refunded ? 'refunded' : 'already_refunded']);
    }
}
