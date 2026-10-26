<?php

namespace App\Part3\Services;

use Illuminate\Support\Facades\DB;
use RuntimeException;

/**
 * Applies a gateway settlement report: every settled payment is allocated
 * to the plan whose installment it collected.
 */
class SettlementImporter
{
    public function __construct(
        private readonly SettlementReportReader $reader,
        private readonly LedgerFileLock $locks,
        private readonly PaymentAllocator $allocator,
    ) {
    }

    /**
     * @return int|null number of payments applied, null when another worker owns the report
     */
    public function import(string $reportPath, string $reportId): ?int
    {
        $lock = $this->locks->acquire($reportId);

        if ($lock === null) {
            return null;
        }

        try {
            $stream = fopen($reportPath, 'rb') ?: throw new RuntimeException('Settlement report is not readable.');

            try {
                $entries = $this->reader->read($stream, $reportId);
            } finally {
                fclose($stream);
            }

            $applied = 0;
            foreach ($entries as $entry) {
                // Our debit references look like "inst-<installment id>-<yyyymmdd>".
                if (preg_match('/^inst-(\d+)-\d{8}$/', $entry['payment_reference'], $m) !== 1) {
                    continue;
                }

                $planId = DB::table('plan_installments')->where('id', (int) $m[1])->value('repayment_plan_id');

                if ($planId !== null) {
                    $this->allocator->allocate((int) $planId, $entry['amount_minor']);
                    $applied++;
                }
            }

            return $applied;
        } finally {
            $this->locks->release($lock);
        }
    }
}
