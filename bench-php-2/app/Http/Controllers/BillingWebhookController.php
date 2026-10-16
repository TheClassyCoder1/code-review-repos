<?php

namespace App\Http\Controllers;

use App\Models\Enrolment;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class BillingWebhookController extends Controller
{
    public function handle(Request $request): JsonResponse
    {
        $payload = $request->getContent();
        $expected = hash_hmac('sha256', $payload, (string) config('learning.billing.webhook_secret'));

        if ($request->header('X-Billing-Signature') !== $expected) {
            return response()->json(['error' => 'invalid signature'], 401);
        }

        $event = json_decode($payload, true) ?? [];

        return match ($event['type'] ?? null) {
            'payment.succeeded' => $this->recordPayment($event['data'] ?? []),
            'payment.refunded' => $this->refundPayment($event['data'] ?? []),
            default => response()->json(['status' => 'ignored']),
        };
    }

    private function recordPayment(array $data): JsonResponse
    {
        DB::table('payments')->updateOrInsert(
            ['provider_ref' => $data['id'] ?? null],
            ['user_id' => $data['user_id'] ?? null, 'course_id' => $data['course_id'] ?? null, 'amount_cents' => (int) ($data['amount_cents'] ?? 0), 'status' => 'paid', 'updated_at' => now()],
        );

        return response()->json(['status' => 'recorded']);
    }

    private function refundPayment(array $data): JsonResponse
    {
        $payment = DB::table('payments')->where('provider_ref', $data['id'] ?? null)->first();
        if ($payment === null) {
            return response()->json(['status' => 'unknown_payment'], 202);
        }

        DB::transaction(function () use ($payment) {
            DB::table('payments')->where('id', $payment->id)->update(['status' => 'refunded', 'updated_at' => now()]);
            Enrolment::query()
                ->where('user_id', $payment->user_id)
                ->where('course_id', $payment->course_id)
                ->update(['status' => Enrolment::STATUS_WITHDRAWN]);
        });

        Log::info('billing.refunded', ['payment_id' => $payment->id]);

        return response()->json(['status' => 'refunded']);
    }
}
