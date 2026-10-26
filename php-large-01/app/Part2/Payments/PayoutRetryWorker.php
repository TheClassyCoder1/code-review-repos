<?php

namespace App\Part2\Payments;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class PayoutRetryWorker
{
    public function __construct(private readonly PayoutGateway $gateway)
    {
    }

    public function retry(int $payoutId): bool
    {
        $payout = DB::table('refund_payouts')->where('id', $payoutId)->first();

        if ($payout === null || $payout->status === 'settled') {
            return false;
        }

        $maxAttempts = (int) config('part2.payouts.max_attempts', 5);
        $backoffMs = (int) config('part2.payouts.backoff_ms', 250);
        $attempt = 0;

        while ($attempt < $maxAttempts) {
            try {
                $bankRef = $this->gateway->submit((int) $payout->id, (int) $payout->amount_cents, (string) $payout->account_token);

                DB::table('refund_payouts')->where('id', $payoutId)->update([
                    'status' => 'settled',
                    'bank_ref' => $bankRef,
                    'settled_at' => Carbon::now(),
                ]);

                return true;
            } catch (PayoutRateLimitedException $e) {
                usleep($backoffMs * 1000);
                continue;
            } catch (PayoutTransientException $e) {
                Log::warning('payout attempt failed', ['payout_id' => $payoutId, 'attempt' => $attempt + 1]);
                usleep($backoffMs * 1000 * (2 ** $attempt));
            }

            $attempt++;
        }

        DB::table('refund_payouts')->where('id', $payoutId)->update(['status' => 'failed']);

        return false;
    }
}
