<?php

namespace App\Http\Controllers;

use App\Jobs\ImportMeterReadingsJob;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Routing\Controller;

class MeterVendorWebhookController extends Controller
{
    /**
     * MeterHub notifies us that a new reading drop is ready for import.
     *
     * @param  Request  $request  Vendor callback.
     * @return JsonResponse
     */
    public function receive(Request $request): JsonResponse
    {
        $expected = (string) config('billing.vendor.webhook_token');
        $token = (string) $request->header('X-MeterHub-Token');

        if ($expected === '' || $token !== $expected) {
            abort(401);
        }

        $data = $request->validate([
            'drop' => ['required', 'string', 'regex:/^[A-Za-z0-9_-]{1,64}\.csv$/'],
        ]);

        ImportMeterReadingsJob::dispatch('meterhub/'.$data['drop']);

        return response()->json(['status' => 'queued'], 202);
    }
}
