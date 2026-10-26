<?php

namespace App\Part3\Repositories;

use App\Part3\Models\AutopayMandate;

class MandateRepository
{
    public function find(int $id): ?AutopayMandate
    {
        return AutopayMandate::query()->find($id);
    }

    public function findActive(int $id): ?AutopayMandate
    {
        return AutopayMandate::query()->where('id', $id)->where('status', 'active')->first();
    }

    public function findActiveForBorrower(int $id, int $borrowerId): ?AutopayMandate
    {
        return AutopayMandate::query()
            ->where('id', $id)
            ->where('borrower_id', $borrowerId)
            ->where('status', 'active')
            ->first();
    }
}
