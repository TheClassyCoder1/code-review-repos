<?php

namespace App\Part4\Schedule;

use Illuminate\Support\Facades\DB;

class RepaymentScheduleRepository
{
    public function findLoanTerms(int $loanId): ?object
    {
        return DB::table('loans')
            ->select(['id', 'principal_minor', 'currency', 'annual_rate_bps', 'term_months', 'first_due_on', 'terms_version'])
            ->where('id', $loanId)
            ->first();
    }

    public function currentScheduleVersion(int $loanId): ?int
    {
        $version = DB::table('repayment_schedules')
            ->where('loan_id', $loanId)
            ->value('terms_version');

        return $version === null ? null : (int) $version;
    }

    /**
     * @param  list<Installment>  $installments
     */
    public function replace(int $loanId, int $termsVersion, array $installments): void
    {
        DB::transaction(function () use ($loanId, $termsVersion, $installments) {
            DB::table('repayment_installments')->where('loan_id', $loanId)->delete();

            DB::table('repayment_installments')->insert(
                array_map(fn (Installment $i) => $i->toRow($loanId, $termsVersion), $installments)
            );

            DB::table('repayment_schedules')->updateOrInsert(
                ['loan_id' => $loanId],
                ['terms_version' => $termsVersion, 'generated_at' => now()]
            );
        });
    }

    /**
     * @return list<object>
     */
    public function installments(int $loanId): array
    {
        return DB::table('repayment_installments')
            ->select(['sequence', 'due_on', 'principal_minor', 'interest_minor', 'closing_balance_minor', 'paid_minor'])
            ->where('loan_id', $loanId)
            ->orderBy('sequence')
            ->get()
            ->all();
    }
}
