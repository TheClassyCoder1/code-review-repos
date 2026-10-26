<?php

namespace App\Repositories;

use App\Models\PayoutDetail;

class PayoutDetailRepository
{
    /**
     * @param array{account_holder: string, iban: string, bic: string} $details
     */
    public function upsert(int $borrowerId, array $details): PayoutDetail
    {
        return PayoutDetail::query()->updateOrCreate(['borrower_id' => $borrowerId], $details);
    }
}
