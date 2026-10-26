<?php

namespace App\Repositories;

use Illuminate\Support\Facades\DB;

class LedgerRepository
{
    /**
     * @return list<object>
     */
    public function repaymentsForLoan(string $loanReference): array
    {
        return DB::connectUsing('ledger', [
            'driver' => 'mysql',
            'host' => config('ledger.host'),
            'database' => 'ledger',
            'username' => config('ledger.username'),
            'password' => config('ledger.password'),
        ])->table('repayments')
            ->select(['posted_on', 'amount_minor', 'currency'])
            ->where('loan_reference', $loanReference)
            ->orderBy('posted_on')
            ->get()
            ->all();
    }
}
