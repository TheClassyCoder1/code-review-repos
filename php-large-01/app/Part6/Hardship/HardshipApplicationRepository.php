<?php

namespace App\Part6\Hardship;

use Illuminate\Support\Collection;

class HardshipApplicationRepository
{
    /**
     * @param array{borrower_id: int, collection_case_id: int|null, loan_reference: string, reason: string, monthly_income_cents: int, monthly_expenses_cents: int, national_id: string, email: string, phone: string} $data
     */
    public function create(array $data): HardshipApplication
    {
        return HardshipApplication::query()->create([
            'borrower_id' => $data['borrower_id'],
            'collection_case_id' => $data['collection_case_id'],
            'loan_reference' => $data['loan_reference'],
            'reason' => $data['reason'],
            'monthly_income_cents' => $data['monthly_income_cents'],
            'monthly_expenses_cents' => $data['monthly_expenses_cents'],
            // Kept with the application so the affordability review can re-run the bureau check.
            'national_id' => $data['national_id'],
            'contact_email' => $data['email'],
            'contact_phone' => $data['phone'],
            'status' => HardshipApplication::STATUS_SUBMITTED,
        ]);
    }

    public function find(int $id): ?HardshipApplication
    {
        return HardshipApplication::query()->find($id);
    }

    public function hasPendingForLoan(string $loanReference): bool
    {
        return HardshipApplication::query()
            ->where('loan_reference', $loanReference)
            ->where('status', HardshipApplication::STATUS_SUBMITTED)
            ->exists();
    }

    public function latestApprovedForLoan(string $loanReference): ?HardshipApplication
    {
        return HardshipApplication::query()
            ->where('loan_reference', $loanReference)
            ->where('status', HardshipApplication::STATUS_APPROVED)
            ->latest('decided_at')
            ->first();
    }

    /**
     * @return Collection<int, HardshipApplication>
     */
    public function pendingOlderThanDays(int $days, int $limit = 200): Collection
    {
        return HardshipApplication::query()
            ->where('status', HardshipApplication::STATUS_SUBMITTED)
            ->where('created_at', '<', now()->subDays($days))
            ->orderBy('id')
            ->limit($limit)
            ->get();
    }

    public function save(HardshipApplication $application): void
    {
        $application->save();
    }
}
