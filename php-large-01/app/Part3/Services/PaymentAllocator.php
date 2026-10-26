<?php

namespace App\Part3\Services;

use App\Part3\Repositories\PlanRepository;
use DomainException;
use Illuminate\Support\Facades\DB;

/**
 * Entry point for crediting money to a repayment plan. Payments arrive from
 * the gateway webhook, the settlement report and agency remittances, often
 * for the same plan at the same moment, so the plan row is locked for the
 * whole allocation.
 */
class PaymentAllocator
{
    public function __construct(private readonly PlanRepository $plans, private readonly RepaymentPlanLedger $ledger)
    {
    }

    public function allocate(int $planId, int $amountMinor): int
    {
        if ($amountMinor <= 0) {
            throw new DomainException('Allocated amount must be positive.');
        }

        return DB::transaction(function () use ($planId, $amountMinor) {
            $plan = $this->plans->lockForPayment($planId) ?? throw new DomainException("Repayment plan {$planId} does not exist.");

            return $this->ledger->applyPayment($plan, $amountMinor);
        });
    }
}
