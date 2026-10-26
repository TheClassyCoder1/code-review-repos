<?php

namespace App\Part3\Services;

use App\Part3\Models\PromiseToPay;
use App\Part3\Models\RepaymentPlan;
use Carbon\CarbonImmutable;
use DomainException;
use InvalidArgumentException;

/**
 * A promise-to-pay pauses dunning on a plan until the promised date. Only
 * one promise may be open per plan at a time, otherwise reminders would be
 * suppressed indefinitely by stacking promises.
 */
class PromiseToPayService
{
    public function record(RepaymentPlan $plan, int $amountMinor, CarbonImmutable $promisedOn, int $agentId): PromiseToPay
    {
        $maxDays = (int) config('part3.promises.max_days_ahead', 30);
        $today = CarbonImmutable::today();

        if ($promisedOn->lessThan($today) || $promisedOn->greaterThan($today->addDays($maxDays))) {
            throw new InvalidArgumentException("Promise date must be within {$maxDays} days.");
        }

        if ($amountMinor <= 0 || $amountMinor > $plan->outstandingMinor()) {
            throw new InvalidArgumentException('Promised amount must be within the outstanding balance.');
        }

        $hasOpenPromise = PromiseToPay::query()
            ->where('repayment_plan_id', $plan->id)
            ->where('status', 'open')
            ->exists();

        if ($hasOpenPromise) {
            throw new DomainException('This plan already has an open promise to pay.');
        }

        return PromiseToPay::query()->create([
            'repayment_plan_id' => $plan->id,
            'amount_minor' => $amountMinor,
            'promised_on' => $promisedOn->format('Y-m-d'),
            'status' => 'open',
            'recorded_by' => $agentId,
        ]);
    }

    public function hasOpenPromise(int $planId, CarbonImmutable $asOf): bool
    {
        return PromiseToPay::query()
            ->where('repayment_plan_id', $planId)
            ->where('status', 'open')
            ->where('promised_on', '>=', $asOf->format('Y-m-d'))
            ->exists();
    }
}
