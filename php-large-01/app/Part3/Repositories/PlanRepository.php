<?php

namespace App\Part3\Repositories;

use App\Part3\Models\RepaymentPlan;

class PlanRepository
{
    public function find(int $id): ?RepaymentPlan
    {
        return RepaymentPlan::query()->find($id);
    }

    public function findForBorrower(int $id, int $borrowerId): ?RepaymentPlan
    {
        return RepaymentPlan::query()->where('id', $id)->where('borrower_id', $borrowerId)->first();
    }

    /**
     * Row-locks the plan for the rest of the surrounding transaction.
     */
    public function lockForPayment(int $id): ?RepaymentPlan
    {
        return RepaymentPlan::query()->whereKey($id)->lockForUpdate()->first();
    }

    /**
     * @param array<string, mixed> $attributes
     */
    public function create(array $attributes): RepaymentPlan
    {
        return RepaymentPlan::query()->create($attributes);
    }
}
