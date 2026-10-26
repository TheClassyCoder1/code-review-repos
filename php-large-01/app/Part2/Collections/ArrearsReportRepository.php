<?php

namespace App\Part2\Collections;

use Illuminate\Support\Facades\DB;

class ArrearsReportRepository
{
    public function page(string $orderColumn, string $direction, ?string $bucket, int $limit, int $offset): array
    {
        $sql = 'SELECT a.loan_id, a.borrower_id, a.arrears_cents, a.days_past_due, a.bucket, a.last_payment_at '
            . 'FROM arrears_accounts a '
            . 'WHERE (? IS NULL OR a.bucket = ?) '
            . "ORDER BY {$orderColumn} {$direction}, a.loan_id ASC "
            . 'LIMIT ? OFFSET ?';

        return DB::select($sql, [$bucket, $bucket, $limit, $offset]);
    }

    public function count(?string $bucket): int
    {
        return (int) DB::table('arrears_accounts')
            ->when($bucket !== null, fn ($q) => $q->where('bucket', $bucket))
            ->count();
    }
}
