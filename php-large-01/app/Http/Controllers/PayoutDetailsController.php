<?php

namespace App\Http\Controllers;

use App\Repositories\PayoutDetailRepository;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class PayoutDetailsController extends Controller
{
    public function __construct(
        private readonly PayoutDetailRepository $payouts,
    ) {
    }

    /**
     * Changes where the next disbursement is paid. Session-authenticated
     * portal form post.
     */
    public function update(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        $validated = $request->validate([
            'account_holder' => ['required', 'string', 'max:140'],
            'iban' => ['required', 'string', 'regex:/^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$/'],
            'bic' => ['required', 'string', 'regex:/^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$/'],
        ]);

        $details = $this->payouts->upsert($user->id, $validated);

        Log::info('payout details updated', ['borrower_id' => $user->id, 'payout_detail_id' => $details->id]);

        return new JsonResponse(['status' => 'updated', 'ibanLast4' => substr($details->iban, -4)]);
    }
}
