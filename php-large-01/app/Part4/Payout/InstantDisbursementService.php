<?php

namespace App\Part4\Payout;

use App\Part4\Disbursement\DisbursementRequestRepository;
use App\Part4\Support\Money;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Pushes small disbursements through the real-time rail instead of waiting for
 * the next bulk file, and settles them once the rail confirms.
 */
class InstantDisbursementService
{
    private const INSTANT_RAILS = ['imps', 'sepa'];

    public function __construct(
        private readonly PayoutRailClient $rail,
        private readonly DisbursementRequestRepository $requests,
    ) {
    }

    /**
     * Claims a pending request for instant payout and submits it. The claim is a
     * conditional status update, so a request already exported to a bulk file
     * (or claimed by another worker) is left alone.
     */
    public function submitIfEligible(int $disbursementId): ?string
    {
        $row = DB::table('disbursement_requests')
            ->select(['id', 'amount_minor', 'currency', 'rail', 'beneficiary_account', 'beneficiary_routing'])
            ->where('id', $disbursementId)
            ->first();

        if ($row === null || ! in_array($row->rail, self::INSTANT_RAILS, true)) {
            return null;
        }

        if ((int) $row->amount_minor > (int) config('part4_payouts.max_instant_minor')) {
            return null;
        }

        $claimed = DB::table('disbursement_requests')
            ->where('id', $disbursementId)
            ->where('status', 'pending')
            ->update(['status' => 'submitted', 'updated_at' => now()]);

        if ($claimed !== 1) {
            return null;
        }

        try {
            $result = $this->rail->submitTransfer(
                $disbursementId,
                Money::ofMinor((int) $row->amount_minor, (string) $row->currency),
                decrypt((string) $row->beneficiary_account),
                (string) $row->beneficiary_routing,
            );
        } catch (RuntimeException $e) {
            DB::table('disbursement_requests')
                ->where('id', $disbursementId)
                ->where('status', 'submitted')
                ->update(['status' => 'pending', 'updated_at' => now()]);

            throw $e;
        }

        DB::table('disbursement_requests')
            ->where('id', $disbursementId)
            ->update(['rail_reference' => $result['rail_reference'], 'updated_at' => now()]);

        return $result['rail_reference'];
    }

    /**
     * Polls the rail for submitted transfers and settles or fails them.
     *
     * @return array{settled: int, failed: int, pending: int}
     */
    public function reconcileSubmitted(int $limit = 100): array
    {
        $counts = ['settled' => 0, 'failed' => 0, 'pending' => 0];

        $rows = DB::table('disbursement_requests')
            ->select(['id', 'rail_reference'])
            ->where('status', 'submitted')
            ->whereNotNull('rail_reference')
            ->orderBy('updated_at')
            ->limit($limit)
            ->get();

        foreach ($rows as $row) {
            $status = $this->rail->transferStatus((string) $row->rail_reference);

            if ($status === 'completed' && $this->requests->markSettled((int) $row->id, (string) $row->rail_reference)) {
                $counts['settled']++;
            } elseif (in_array($status, ['rejected', 'returned'], true)) {
                $counts['failed'] += DB::table('disbursement_requests')
                    ->where('id', $row->id)
                    ->where('status', 'submitted')
                    ->update(['status' => 'failed', 'updated_at' => now()]);
            } else {
                $counts['pending']++;
            }
        }

        Log::info('instant disbursements reconciled', $counts);

        return $counts;
    }
}
