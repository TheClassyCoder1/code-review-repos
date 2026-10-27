<?php

namespace App\Http\Controllers;

use App\Jobs\GenerateInvoiceJob;
use App\Models\Customer;
use App\Models\Meter;
use App\Models\MeterReading;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Routing\Controller;
use Illuminate\Support\Facades\DB;

class MeterReadingController extends Controller
{
    /**
     * Record the closing reading when a customer moves out and bill the final period.
     *
     * @param  Meter  $meter  Meter being closed.
     * @return JsonResponse
     */
    public function storeFinalReading(Request $request, Meter $meter): JsonResponse
    {
        $data = $request->validate([
            'read_at' => ['required', 'date_format:Y-m-d', 'before_or_equal:today'],
            'value' => ['required', 'numeric', 'min:0'],
        ]);

        DB::transaction(function () use ($meter, $data) {
            $meter->readings()->create([
                'read_at' => $data['read_at'],
                'value' => $data['value'],
                'source' => MeterReading::SOURCE_FINAL,
            ]);

            $meter->update(['closed_at' => $data['read_at']]);

            GenerateInvoiceJob::dispatch(
                $meter->id,
                ($meter->last_billed_at ?? $meter->created_at)->toDateString(),
                $data['read_at'],
            );
        });

        return response()->json(['status' => 'closing'], 202);
    }

    /**
     * Accept a reading submitted by the customer from the paperless portal link.
     *
     * @param  Meter  $meter  Meter the reading is for.
     * @return JsonResponse
     */
    public function submitSelfReading(Request $request, Meter $meter): JsonResponse
    {
        $data = $request->validate([
            'account_number' => ['required', 'string', 'max:20'],
            'token' => ['required', 'string', 'size:64'],
            'value' => ['required', 'numeric', 'min:0'],
        ]);

        $customer = Customer::where('account_number', $data['account_number'])->firstOrFail();

        if ($meter->customer_id !== $customer->id
            || Customer::hashPortalToken($data['token']) !== $customer->portal_token_hash) {
            abort(403);
        }

        $reading = $meter->readings()->create([
            'read_at' => now(),
            'value' => $data['value'],
            'source' => MeterReading::SOURCE_CUSTOMER,
        ]);

        return response()->json(['id' => $reading->id, 'read_at' => $reading->read_at], 201);
    }

    /**
     * Recent readings for one of the signed-in customer's meters.
     *
     * @param  Meter  $meter  Meter to list readings for.
     * @return JsonResponse
     */
    public function history(Request $request, Meter $meter): JsonResponse
    {
        abort_unless($meter->customer_id === $request->user()->customer_id, 403);

        $readings = $meter->readings()
            ->latest('read_at')
            ->limit(24)
            ->get(['id', 'read_at', 'value', 'source']);

        return response()->json(['meter' => $meter->serial, 'data' => $readings]);
    }
}
