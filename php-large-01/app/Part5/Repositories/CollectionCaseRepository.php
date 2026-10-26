<?php

namespace App\Part5\Repositories;

use App\Part5\Models\CollectionCase;
use App\Part5\Models\PaymentPromise;
use DateTimeInterface;
use Illuminate\Support\Collection;

class CollectionCaseRepository
{
    public function find(int $id): ?CollectionCase
    {
        return CollectionCase::query()->find($id);
    }

    public function findOpenForLoan(string $loanReference): ?CollectionCase
    {
        return CollectionCase::query()
            ->where('loan_reference', $loanReference)
            ->where('status', '!=', CollectionCase::STATUS_CLOSED)
            ->first();
    }

    /**
     * Keyset page of open cases, oldest id first. Using the id cursor instead
     * of OFFSET keeps nightly runs stable while cases are being closed.
     *
     * @param list<string> $buckets
     * @return array{items: Collection<int, CollectionCase>, next_cursor: int|null}
     */
    public function openCasesPage(int $afterId, int $limit, array $buckets = []): array
    {
        $limit = max(1, min($limit, 200));

        $rows = CollectionCase::query()
            ->where('status', '!=', CollectionCase::STATUS_CLOSED)
            ->where('id', '>', max(0, $afterId))
            ->when($buckets !== [], fn ($q) => $q->whereIn('bucket', $buckets))
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
     * @param array<string, mixed> $attributes
     */
    public function create(array $attributes): CollectionCase
    {
        return CollectionCase::query()->create($attributes);
    }

    public function save(CollectionCase $case): void
    {
        $case->save();
    }

    public function countOpenPromises(int $caseId): int
    {
        return PaymentPromise::query()
            ->where('collection_case_id', $caseId)
            ->where('status', PaymentPromise::STATUS_OPEN)
            ->count();
    }

    /**
     * @return Collection<int, PaymentPromise>
     */
    public function openPromisesDueBefore(DateTimeInterface $date): Collection
    {
        return PaymentPromise::query()
            ->with('collectionCase')
            ->where('status', PaymentPromise::STATUS_OPEN)
            ->whereDate('promised_for', '<', $date)
            ->orderBy('id')
            ->get();
    }

    /**
     * @param array<string, mixed> $attributes
     */
    public function createPromise(array $attributes): PaymentPromise
    {
        return PaymentPromise::query()->create($attributes);
    }

    public function sumPaymentsSince(string $loanReference, DateTimeInterface $since): int
    {
        return (int) CollectionCase::query()
            ->getConnection()
            ->table('loan_payments')
            ->where('loan_reference', $loanReference)
            ->where('posted_at', '>=', $since)
            ->sum('amount_cents');
    }
}
