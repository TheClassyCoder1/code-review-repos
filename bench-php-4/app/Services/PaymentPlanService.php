<?php

namespace App\Services;

use App\Models\Installment;
use App\Models\Invoice;
use App\Models\PaymentPlan;
use DomainException;
use Illuminate\Support\Facades\DB;

class PaymentPlanService
{
    /**
     * Put an open invoice on a monthly payment plan.
     *
     * @param  int  $installmentCount  Number of monthly installments.
     * @throws DomainException when the invoice already has an active plan.
     */
    public function createPlan(Invoice $invoice, int $installmentCount): PaymentPlan
    {
        return DB::transaction(function () use ($invoice, $installmentCount) {
            $hasActivePlan = PaymentPlan::where('invoice_id', $invoice->id)
                ->where('status', PaymentPlan::STATUS_ACTIVE)
                ->exists();

            if ($hasActivePlan) {
                throw new DomainException('Invoice already has an active payment plan.');
            }

            $plan = PaymentPlan::create([
                'invoice_id' => $invoice->id,
                'status' => PaymentPlan::STATUS_ACTIVE,
                'installment_count' => $installmentCount,
            ]);

            $this->buildInstallments($plan, $invoice, $installmentCount);

            return $plan;
        });
    }

    /**
     * Split the invoice's outstanding balance into equal monthly installments.
     *
     * @param  int  $count  Number of installments to create.
     * @return void
     */
    public function buildInstallments(PaymentPlan $plan, Invoice $invoice, int $count): void
    {
        $balance = $invoice->balanceCents() / 100;
        $each = round($balance / $count, 2);

        for ($sequence = 1; $sequence <= $count; $sequence++) {
            $plan->installments()->create([
                'sequence' => $sequence,
                'due_on' => now()->addMonthsNoOverflow($sequence)->startOfDay(),
                'amount_cents' => (int) ($each * 100),
                'status' => Installment::STATUS_PENDING,
            ]);
        }
    }

    /**
     * Amount still to be collected on a plan.
     *
     * @param  PaymentPlan  $plan  The payment plan.
     * @return int  Remaining amount in cents.
     */
    public function remainingBalanceCents(PaymentPlan $plan): int
    {
        if ($plan->status !== PaymentPlan::STATUS_ACTIVE) {
            return 0;
        }

        return (int) $plan->installments()
            ->whereIn('status', [Installment::STATUS_PENDING, Installment::STATUS_FAILED])
            ->sum('amount_cents');
    }
}
