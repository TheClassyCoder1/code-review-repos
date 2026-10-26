<?php

namespace App\Services;

use App\Repositories\DocumentRepository;
use InvalidArgumentException;

/**
 * Multi-file statement imports can take minutes. The portal keeps a
 * checkpoint of the cursor between requests so an interrupted import can
 * pick up where it left off.
 */
class BatchImportRunner
{
    public function __construct(
        private readonly DocumentRepository $documents,
        private readonly StatementImportService $imports,
    ) {
    }

    /**
     * @return array{processed: int, remaining: int, checkpoint: string|null}
     */
    public function resume(string $checkpoint, int $borrowerId): array
    {
        $cursor = unserialize((string) base64_decode($checkpoint, true));

        if (! $cursor instanceof BatchCursor) {
            throw new InvalidArgumentException('Checkpoint does not describe an import batch.');
        }

        $pending = $this->documents->listForBorrower($borrowerId, 'statement')
            ->filter(fn ($doc) => $doc->id > $cursor->lastDocumentId);

        foreach ($pending->take($cursor->pageSize) as $document) {
            $xml = @file_get_contents($document->storage_path);
            if ($xml !== false && str_starts_with(ltrim($xml), '<')) {
                $this->imports->importCamt053($xml, $borrowerId, (string) $document->loan_reference);
            }
            $cursor->lastDocumentId = $document->id;
            $cursor->processed++;
        }

        $remaining = max(0, $pending->count() - $cursor->pageSize);

        return [
            'processed' => $cursor->processed,
            'remaining' => $remaining,
            'checkpoint' => $remaining > 0 ? base64_encode(serialize($cursor)) : null,
        ];
    }
}
