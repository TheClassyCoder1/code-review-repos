<?php

namespace App\Part4\Remittance;

use Illuminate\Support\Facades\DB;

class RemittanceRepository
{
    /**
     * @param  list<string>  $loanReferences
     * @return array<string, list<object>> loan reference => open installments, oldest first
     */
    public function openInstallmentsByLoan(array $loanReferences): array
    {
        if ($loanReferences === []) {
            return [];
        }

        $rows = DB::table('repayment_installments as i')
            ->join('loans as l', 'l.id', '=', 'i.loan_id')
            ->select(['l.loan_reference', 'i.id', 'i.loan_id', 'i.sequence', 'i.due_on', 'i.principal_minor', 'i.interest_minor', 'i.paid_minor'])
            ->whereIn('l.loan_reference', $loanReferences)
            ->whereRaw('i.paid_minor < i.principal_minor + i.interest_minor')
            ->orderBy('i.loan_id')
            ->orderBy('i.sequence')
            ->get();

        $grouped = [];

        foreach ($rows as $row) {
            $grouped[(string) $row->loan_reference][] = $row;
        }

        return $grouped;
    }

    /**
     * @param  list<string>  $partnerReferences
     * @return list<string>
     */
    public function alreadyApplied(string $partnerCode, array $partnerReferences): array
    {
        if ($partnerReferences === []) {
            return [];
        }

        return DB::table('remittance_receipts')
            ->where('partner_code', $partnerCode)
            ->whereIn('partner_reference', $partnerReferences)
            ->pluck('partner_reference')
            ->map(fn ($ref) => (string) $ref)
            ->all();
    }

    /**
     * @param  list<array{installment_id: int, applied_minor: int}>  $allocations
     */
    public function applyReceipt(string $partnerCode, RemittanceLine $line, array $allocations, int $unappliedMinor): void
    {
        DB::transaction(function () use ($partnerCode, $line, $allocations, $unappliedMinor) {
            $receiptId = DB::table('remittance_receipts')->insertGetId([
                'partner_code' => $partnerCode,
                'partner_reference' => $line->partnerReference,
                'loan_reference' => $line->loanReference,
                'amount_minor' => $line->amount->minor,
                'currency' => $line->amount->currency,
                'value_date' => $line->valueDate->format('Y-m-d'),
                'unapplied_minor' => $unappliedMinor,
                'created_at' => now(),
            ]);

            foreach ($allocations as $allocation) {
                DB::table('repayment_installments')
                    ->where('id', $allocation['installment_id'])
                    ->increment('paid_minor', $allocation['applied_minor']);

                DB::table('remittance_allocations')->insert([
                    'receipt_id' => $receiptId,
                    'installment_id' => $allocation['installment_id'],
                    'applied_minor' => $allocation['applied_minor'],
                ]);
            }
        });
    }
}
