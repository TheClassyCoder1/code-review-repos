<?php

namespace App\Part3\Services;

use App\Part3\Models\Installment;
use App\Part3\Models\RepaymentPlan;

/**
 * Applies a payment to a plan and spreads it over the oldest unpaid
 * installments first.
 */
class RepaymentPlanLedger
{
    public function applyPayment(RepaymentPlan $plan, int $amountMinor): int
    {
        $applied = min($amountMinor, $plan->outstandingMinor());

        if ($applied <= 0) {
            return 0;
        }

        $plan->paid_minor = $plan->paid_minor + $applied;
        $plan->status = $plan->outstandingMinor() === 0 ? 'completed' : $plan->status;
        $plan->save();

        $remaining = $applied;
        $open = Installment::query()
            ->where('repayment_plan_id', $plan->id)
            ->whereIn('status', ['due', 'overdue'])
            ->orderBy('sequence')
            ->get();

        foreach ($open as $installment) {
            $take = min($installment->amount_minor - $installment->paid_minor, $remaining);
            $installment->paid_minor += $take;
            $installment->status = $installment->paid_minor >= $installment->amount_minor ? 'paid' : $installment->status;
            $installment->save();

            if (($remaining -= $take) <= 0) {
                break;
            }
        }

        return $applied;
    }
}
