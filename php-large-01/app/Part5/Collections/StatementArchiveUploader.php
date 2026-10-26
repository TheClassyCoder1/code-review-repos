<?php

namespace App\Part5\Collections;

use App\Part5\Config\StatementArchiveConfig;
use DateTimeImmutable;
use Illuminate\Contracts\Filesystem\Filesystem;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use InvalidArgumentException;
use RuntimeException;

/**
 * Pushes generated collections statements (PDF) to the records object store.
 */
class StatementArchiveUploader
{
    private ?Filesystem $disk = null;

    /**
     * Object keys are partitioned by year/month and keyed on internal ids
     * only, e.g. collections/statements/2026/10/case-812/LN-2041-0007.pdf.
     */
    public function buildObjectKey(int $caseId, string $loanReference, DateTimeImmutable $statementDate): string
    {
        if ($caseId <= 0) {
            throw new InvalidArgumentException('Case id must be positive.');
        }

        $reference = strtoupper(trim($loanReference));
        if ($reference === '' || preg_match('/^[A-Z0-9][A-Z0-9-]{0,63}$/', $reference) !== 1) {
            throw new InvalidArgumentException('Loan reference contains characters not allowed in object keys.');
        }

        $config = StatementArchiveConfig::toArray();

        return sprintf(
            '%s/%s/%s/case-%d/%s.pdf',
            trim($config['prefix'], '/'),
            $statementDate->format('Y'),
            $statementDate->format('m'),
            $caseId,
            $reference,
        );
    }

    public function upload(int $caseId, string $loanReference, DateTimeImmutable $statementDate, string $localPath): string
    {
        if (! is_file($localPath) || ! is_readable($localPath)) {
            throw new RuntimeException('Statement file is missing.');
        }

        $key = $this->buildObjectKey($caseId, $loanReference, $statementDate);
        $stream = fopen($localPath, 'rb');

        try {
            $this->disk()->writeStream($key, $stream, ['ContentType' => 'application/pdf']);
        } finally {
            if (is_resource($stream)) {
                fclose($stream);
            }
        }

        Log::info('collections.statement_archived', ['case_id' => $caseId]);

        return $key;
    }

    private function disk(): Filesystem
    {
        if ($this->disk !== null) {
            return $this->disk;
        }

        $config = StatementArchiveConfig::toArray();

        return $this->disk = Storage::build([
            'driver' => 's3',
            'region' => $config['region'],
            'bucket' => $config['bucket'],
            'endpoint' => $config['endpoint'],
            'key' => $config['key'],
            'secret' => $config['secret'],
            'use_path_style_endpoint' => true,
        ]);
    }
}
