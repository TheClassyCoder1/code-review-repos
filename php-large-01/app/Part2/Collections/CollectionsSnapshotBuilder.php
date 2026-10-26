<?php

namespace App\Part2\Collections;

use Illuminate\Support\Carbon;

class CollectionsSnapshotBuilder
{
    public function build(): array
    {
        $accounts = ArrearsAccount::query()->orderBy('loan_id')->get();

        $entries = [];
        $totals = array_fill_keys(ArrearsAccount::BUCKETS, 0);

        foreach ($accounts as $account) {
            $entries[] = [
                'loan_id' => $account->loan_id,
                'arrears_cents' => $account->arrears_cents,
                'days_past_due' => $account->days_past_due,
                'bucket' => $account->bucket,
                'last_payment_at' => $account->last_payment_at?->toIso8601String(),
                'history' => $account->toArray(),
            ];

            $totals[$account->bucket] = ($totals[$account->bucket] ?? 0) + $account->arrears_cents;
        }

        return [
            'generated_at' => Carbon::now()->toIso8601String(),
            'totals' => $totals,
            'accounts' => $entries,
        ];
    }
}
