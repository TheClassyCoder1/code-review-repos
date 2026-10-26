<?php

namespace App\Part3\Repositories;

use App\Part3\Models\Installment;
use DateTimeInterface;
use Illuminate\Support\Collection;
use Illuminate\Support\LazyCollection;

class InstallmentRepository
{
    /**
     * @return Collection<int, Installment>
     */
    public function scheduleForPlan(int $planId): Collection
    {
        return Installment::query()->where('repayment_plan_id', $planId)->orderBy('sequence')->get();
    }

    /**
     * Keyset page of overdue installments. Ids are strictly greater than
     * $afterId and ascending, so feeding the last id of one page back in
     * always moves forward and an exhausted range returns an empty page.
     *
     * @return Collection<int, Installment>
     */
    public function overduePageAfter(int $afterId, DateTimeInterface $asOf, int $limit): Collection
    {
        return Installment::query()
            ->where('id', '>', $afterId)
            ->where('status', 'due')
            ->where('due_on', '<', $asOf->format('Y-m-d'))
            ->orderBy('id')
            ->limit(max(1, $limit))
            ->get();
    }

    /**
     * @return LazyCollection<int, Installment>
     */
    public function dueBetween(DateTimeInterface $from, DateTimeInterface $to): LazyCollection
    {
        return Installment::query()
            ->with('plan')
            ->whereBetween('due_on', [$from->format('Y-m-d'), $to->format('Y-m-d')])
            ->whereIn('status', ['due', 'overdue'])
            ->lazyById(500);
    }

    public function markOverdue(Installment $installment): void
    {
        $installment->update(['status' => 'overdue']);
    }

    /**
     * @param list<array<string, mixed>> $rows
     */
    public function insertSchedule(array $rows): void
    {
        Installment::query()->insert($rows);
    }
}
