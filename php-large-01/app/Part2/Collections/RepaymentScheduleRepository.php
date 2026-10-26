<?php

namespace App\Part2\Collections;

use Illuminate\Support\Facades\DB;

class RepaymentScheduleRepository
{
    public function forLoan(int $loanId): array
    {
        return DB::table('repayment_instalments')
            ->select(['sequence', 'due_on', 'principal_cents', 'interest_cents', 'status'])
            ->where('loan_id', $loanId)
            ->orderBy('sequence')
            ->get()
            ->all();
    }

    public function outstandingPrincipal(int $loanId): int
    {
        return (int) DB::table('repayment_instalments')
            ->where('loan_id', $loanId)
            ->where('status', '!=', 'paid')
            ->sum('principal_cents');
    }
}
