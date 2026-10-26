<?php

namespace App\Part4\Disbursement;

use Illuminate\Support\Facades\DB;

class DisbursementRequestRepository
{
    public function findByIdempotencyKey(string $key): ?object
    {
        return DB::table('disbursement_requests')
            ->select(['id', 'loan_reference', 'amount_minor', 'currency', 'rail', 'status', 'created_at'])
            ->where('idempotency_key', $key)
            ->first();
    }

    public function insert(DisbursementRequestData $data, string $maskedAccount): int
    {
        return (int) DB::table('disbursement_requests')->insertGetId([
            'idempotency_key' => $data->idempotencyKey,
            'loan_reference' => $data->loanReference,
            'borrower_id' => $data->borrowerId,
            'amount_minor' => $data->amount->minor,
            'currency' => $data->amount->currency,
            'beneficiary_account' => encrypt($data->beneficiaryAccount),
            'beneficiary_account_masked' => $maskedAccount,
            'beneficiary_routing' => $data->beneficiaryRouting,
            'rail' => $data->rail,
            'status' => 'pending',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }

    public function find(int $id): ?object
    {
        return DB::table('disbursement_requests')
            ->select(['id', 'loan_reference', 'amount_minor', 'currency', 'rail', 'status', 'created_at'])
            ->where('id', $id)
            ->first();
    }

    /**
     * @return list<object>
     */
    public function pendingForRail(string $rail, int $limit): array
    {
        return DB::table('disbursement_requests')
            ->select(['id', 'loan_reference', 'amount_minor', 'currency', 'beneficiary_account', 'beneficiary_routing'])
            ->where('rail', $rail)
            ->where('status', 'pending')
            ->orderBy('id')
            ->limit($limit)
            ->get()
            ->all();
    }

    /**
     * @param  list<int>  $ids
     */
    public function markExported(array $ids, string $batchReference): int
    {
        if ($ids === []) {
            return 0;
        }

        return DB::table('disbursement_requests')
            ->whereIn('id', $ids)
            ->where('status', 'pending')
            ->update(['status' => 'exported', 'batch_reference' => $batchReference, 'updated_at' => now()]);
    }

    public function markSettled(int $id, string $railReference): bool
    {
        return DB::table('disbursement_requests')
            ->where('id', $id)
            ->whereIn('status', ['exported', 'submitted'])
            ->update(['status' => 'settled', 'rail_reference' => $railReference, 'updated_at' => now()]) === 1;
    }
}
