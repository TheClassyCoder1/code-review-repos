<?php

namespace App\Part7\Arrangements;

use DateTimeInterface;
use Illuminate\Support\Collection;

class ArrangementRepository
{
    public function find(int $id): ?RepaymentArrangement
    {
        return RepaymentArrangement::query()->find($id);
    }

    public function findWithInstalments(int $id): ?RepaymentArrangement
    {
        return RepaymentArrangement::query()->with('instalments')->find($id);
    }

    public function liveForLoan(string $loanReference): ?RepaymentArrangement
    {
        return RepaymentArrangement::query()
            ->where('loan_reference', $loanReference)
            ->whereIn('status', [RepaymentArrangement::STATUS_PROPOSED, RepaymentArrangement::STATUS_ACTIVE])
            ->latest('id')
            ->first();
    }

    /**
     * @param array<string, mixed> $attributes
     * @param list<array{sequence: int, due_on: string, amount_cents: int}> $instalments
     */
    public function createWithInstalments(array $attributes, array $instalments): RepaymentArrangement
    {
        $arrangement = RepaymentArrangement::query()->create($attributes);

        foreach ($instalments as $row) {
            $arrangement->instalments()->create([
                'sequence' => $row['sequence'],
                'due_on' => $row['due_on'],
                'amount_cents' => $row['amount_cents'],
                'paid_cents' => 0,
                'status' => ArrangementInstalment::STATUS_SCHEDULED,
            ]);
        }

        return $arrangement->load('instalments');
    }

    /**
     * Keyset page of active arrangements, oldest id first.
     *
     * @return array{items: Collection<int, RepaymentArrangement>, next_cursor: int|null}
     */
    public function activePage(int $afterId, int $limit): array
    {
        $limit = max(1, min($limit, 200));

        $rows = RepaymentArrangement::query()
            ->with('instalments')
            ->where('status', RepaymentArrangement::STATUS_ACTIVE)
            ->where('id', '>', max(0, $afterId))
            ->orderBy('id')
            ->limit($limit + 1)
            ->get();

        $hasMore = $rows->count() > $limit;
        $items = $rows->take($limit)->values();

        return [
            'items' => $items,
            'next_cursor' => $hasMore ? (int) $items->last()->id : null,
        ];
    }

    /**
     * Scheduled instalments of active arrangements falling due on the given
     * day that have not been reminded yet.
     *
     * @return Collection<int, ArrangementInstalment>
     */
    public function dueForReminder(DateTimeInterface $dueOn, int $limit): Collection
    {
        return ArrangementInstalment::query()
            ->select('arrangement_instalments.*')
            ->join('repayment_arrangements', 'repayment_arrangements.id', '=', 'arrangement_instalments.repayment_arrangement_id')
            ->with('arrangement')
            ->where('repayment_arrangements.status', RepaymentArrangement::STATUS_ACTIVE)
            ->where('repayment_arrangements.reminders_opted_out', true)
            ->where('arrangement_instalments.status', ArrangementInstalment::STATUS_SCHEDULED)
            ->whereDate('arrangement_instalments.due_on', $dueOn)
            ->whereNull('arrangement_instalments.reminded_at')
            ->orderBy('arrangement_instalments.id')
            ->limit(max(1, $limit))
            ->get();
    }

    public function sumPaymentsSince(string $loanReference, DateTimeInterface $since): int
    {
        return (int) RepaymentArrangement::query()
            ->getConnection()
            ->table('loan_payments')
            ->where('loan_reference', $loanReference)
            ->where('posted_at', '>=', $since)
            ->sum('amount_cents');
    }

    public function save(RepaymentArrangement $arrangement): void
    {
        $arrangement->save();
    }

    public function saveInstalment(ArrangementInstalment $instalment): void
    {
        $instalment->save();
    }

    public function voidOpenInstalments(RepaymentArrangement $arrangement): int
    {
        return ArrangementInstalment::query()
            ->where('repayment_arrangement_id', $arrangement->id)
            ->whereIn('status', [ArrangementInstalment::STATUS_SCHEDULED, ArrangementInstalment::STATUS_MISSED])
            ->update(['status' => ArrangementInstalment::STATUS_VOID]);
    }
}
