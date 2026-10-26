<?php

namespace App\Part2\Collections;

use Illuminate\Support\Facades\DB;

class LoanOwnershipRepository
{
    public function borrowerOwns(int $loanId, int $borrowerId): bool
    {
        return DB::table('loans')
            ->where('id', $loanId)
            ->where('borrower_id', $borrowerId)
            ->exists();
    }
}
