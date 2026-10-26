<?php

namespace App\Part3\Services;

use App\Part3\Repositories\InstallmentRepository;
use Carbon\CarbonImmutable;

/**
 * Daily sweep that flips installments past their due date to "overdue" so
 * dunning picks them up. Walks the table in keyset pages.
 */
class OverdueSweep
{
    public function __construct(
        private readonly InstallmentRepository $installments,
        private readonly PromiseToPayService $promises,
    ) {
    }

    public function run(CarbonImmutable $asOf): int
    {
        $lastId = 0;
        $flagged = 0;

        while (true) {
            $page = $this->installments->overduePageAfter($lastId, $asOf, 500);

            if ($page->isEmpty()) {
                break;
            }

            foreach ($page as $installment) {
                $lastId = $installment->id;

                if (! $this->promises->hasOpenPromise($installment->repayment_plan_id, $asOf)) {
                    $this->installments->markOverdue($installment);
                    $flagged++;
                }
            }
        }

        return $flagged;
    }
}
