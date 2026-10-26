<?php

namespace App\Part4\Remittance;

use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\Log;
use InvalidArgumentException;

/**
 * Imports one remittance bundle end to end: manifest check, parse, match and
 * apply. The importer owns the archive handle returned by the reader and always
 * closes it, whether the import succeeds or fails part-way.
 */
class RemittanceBundleImporter
{
    private const CHUNK = 200;

    public function __construct(
        private readonly RemittanceBundleReader $reader,
        private readonly RemittanceRepository $repository,
        private readonly RemittanceMatcher $matcher,
    ) {
    }

    /**
     * @return array{applied: int, duplicates: int, rejected: int, unapplied_minor: int, discrepancies: list<string>}
     */
    public function import(string $bundlePath, string $partnerCode, string $defaultCurrency): array
    {
        $zip = $this->reader->open($bundlePath);

        try {
            $declared = $this->reader->manifest($zip);

            if ($declared === []) {
                throw new InvalidArgumentException('Remittance manifest lists no CSV files.');
            }

            $summary = ['applied' => 0, 'duplicates' => 0, 'rejected' => 0, 'unapplied_minor' => 0];
            $parsedCounts = [];

            foreach (array_keys($declared) as $file) {
                $parsedCounts[$file] = 0;
                $chunk = [];

                foreach ($this->reader->rows($zip, $file) as $row) {
                    $parsedCounts[$file]++;

                    try {
                        $chunk[] = RemittanceLine::fromCsvRow($row, $defaultCurrency);
                    } catch (InvalidArgumentException) {
                        $summary['rejected']++;
                    }

                    if (count($chunk) >= self::CHUNK) {
                        $summary = $this->applyChunk($partnerCode, $chunk, $summary);
                        $chunk = [];
                    }
                }

                $summary = $this->applyChunk($partnerCode, $chunk, $summary);
            }
        } finally {
            $zip->close();
        }

        $summary['discrepancies'] = $this->matcher->manifestDiscrepancies($declared, $parsedCounts);

        Log::info('remittance bundle imported', ['partner_code' => $partnerCode] + array_diff_key($summary, ['discrepancies' => true]));

        return $summary;
    }

    /**
     * @param  list<RemittanceLine>  $lines
     * @param  array{applied: int, duplicates: int, rejected: int, unapplied_minor: int}  $summary
     * @return array{applied: int, duplicates: int, rejected: int, unapplied_minor: int}
     */
    private function applyChunk(string $partnerCode, array $lines, array $summary): array
    {
        if ($lines === []) {
            return $summary;
        }

        $seen = array_flip($this->repository->alreadyApplied(
            $partnerCode,
            array_map(fn (RemittanceLine $l) => $l->partnerReference, $lines)
        ));
        $open = $this->repository->openInstallmentsByLoan(
            array_values(array_unique(array_map(fn (RemittanceLine $l) => $l->loanReference, $lines)))
        );

        foreach ($lines as $line) {
            if (isset($seen[$line->partnerReference])) {
                $summary['duplicates']++;
                continue;
            }

            $plan = $this->matcher->allocate($line, $open[$line->loanReference] ?? []);

            try {
                $this->repository->applyReceipt($partnerCode, $line, $plan['allocations'], $plan['unapplied_minor']);
            } catch (UniqueConstraintViolationException) {
                $summary['duplicates']++;
                continue;
            }

            $seen[$line->partnerReference] = true;
            $open[$line->loanReference] = $this->reduceOpen($open[$line->loanReference] ?? [], $plan['allocations']);
            $summary['applied']++;
            $summary['unapplied_minor'] += $plan['unapplied_minor'];
        }

        return $summary;
    }

    /**
     * Keeps the in-memory open installment list in step with what was just
     * applied, so two receipts for the same loan in one chunk do not both pay
     * the same installment.
     *
     * @param  list<object>  $installments
     * @param  list<array{installment_id: int, applied_minor: int}>  $allocations
     * @return list<object>
     */
    private function reduceOpen(array $installments, array $allocations): array
    {
        $applied = array_column($allocations, 'applied_minor', 'installment_id');

        foreach ($installments as $installment) {
            if (isset($applied[(int) $installment->id])) {
                $installment->paid_minor = (int) $installment->paid_minor + $applied[(int) $installment->id];
            }
        }

        return $installments;
    }
}
