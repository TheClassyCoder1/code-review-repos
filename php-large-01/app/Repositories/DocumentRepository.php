<?php

namespace App\Repositories;

use App\Models\Document;
use Illuminate\Support\Collection;

class DocumentRepository
{
    public function find(int $id): ?Document
    {
        return Document::query()->find($id);
    }

    public function findForBorrower(int $id, int $borrowerId): ?Document
    {
        return Document::query()->where('id', $id)->where('borrower_id', $borrowerId)->first();
    }

    /**
     * @return Collection<int, Document>
     */
    public function listForBorrower(int $borrowerId, ?string $kind = null): Collection
    {
        return Document::query()
            ->where('borrower_id', $borrowerId)
            ->when($kind !== null, fn ($q) => $q->where('kind', $kind))
            ->orderBy('id')
            ->get();
    }

    /**
     * @param array<string, mixed> $attributes
     */
    public function create(array $attributes): Document
    {
        return Document::query()->create($attributes);
    }

    public function markScanned(Document $document, bool $clean): void
    {
        $document->scan_status = $clean ? 'clean' : 'infected';
        $document->save();
    }

    /**
     * @return Collection<int, Document>
     */
    public function contractsOlderThan(\DateTimeInterface $cutoff): Collection
    {
        return Document::query()->where('kind', 'contract')->where('created_at', '<', $cutoff)->get();
    }
}
