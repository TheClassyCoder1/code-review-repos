<?php

namespace App\Part5\Collections;

use App\Part5\Models\CollectionCase;
use App\Part5\Repositories\CollectionCaseRepository;
use DateTimeImmutable;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Writes the nightly hand-over file of seriously delinquent cases for the
 * external collections agency. The agency resolves borrower contact data
 * through its own API using the internal borrower id, so the file carries
 * identifiers and balances only.
 */
class AgencyExportWriter
{
    private const HEADER = ['case_id', 'borrower_id', 'loan_reference', 'bucket', 'days_past_due', 'arrears', 'last_notice'];

    public function __construct(
        private readonly CollectionCaseRepository $cases,
        private readonly string $directory,
        private readonly int $minDaysPastDue,
    ) {
    }

    /**
     * Streams eligible cases into a temp file and atomically renames it, so
     * the agency's poller never picks up a half-written export.
     *
     * @return array{path: string, rows: int}
     */
    public function write(DateTimeImmutable $runDate): array
    {
        if (! is_dir($this->directory) && ! mkdir($this->directory, 0750, true) && ! is_dir($this->directory)) {
            throw new RuntimeException('Cannot create agency export directory.');
        }

        $finalPath = sprintf('%s/agency-%s.csv', rtrim($this->directory, '/'), $runDate->format('Ymd'));
        $tempPath = $finalPath . '.part';
        $handle = fopen($tempPath, 'wb');
        if ($handle === false) {
            throw new RuntimeException('Cannot open agency export for writing.');
        }

        $rows = 0;
        try {
            fputcsv($handle, self::HEADER);
            $cursor = 0;
            do {
                $page = $this->cases->openCasesPage($cursor, 200, ['90+']);
                foreach ($page['items'] as $case) {
                    if ($case->status !== CollectionCase::STATUS_HARDSHIP && $case->days_past_due >= $this->minDaysPastDue) {
                        fputcsv($handle, $this->rowFor($case));
                        $rows++;
                    }
                }
                $cursor = $page['next_cursor'];
            } while ($cursor !== null);
        } finally {
            fclose($handle);
        }

        if (! rename($tempPath, $finalPath)) {
            throw new RuntimeException('Cannot publish agency export.');
        }

        Log::info('collections.agency_export_written', ['rows' => $rows, 'run_date' => $runDate->format('Y-m-d')]);

        return ['path' => $finalPath, 'rows' => $rows];
    }

    /**
     * @return list<string>
     */
    public function rowFor(CollectionCase $case): array
    {
        $arrears = sprintf(
            '%s%d.%02d',
            $case->arrears_cents < 0 ? '-' : '',
            intdiv(abs($case->arrears_cents), 100),
            abs($case->arrears_cents) % 100,
        );

        return [
            (string) $case->id,
            (string) $case->borrower_id,
            $this->neutraliseFormula($case->loan_reference),
            $this->neutraliseFormula($case->bucket),
            (string) $case->days_past_due,
            $arrears,
            $this->neutraliseFormula($case->last_notice ?? ''),
        ];
    }

    /**
     * Spreadsheet apps execute text cells starting with these characters;
     * prefix them so a crafted loan reference cannot become a formula.
     * Numeric columns are formatted by us and left untouched.
     */
    private function neutraliseFormula(string $cell): string
    {
        if ($cell !== '' && in_array($cell[0], ['=', '+', '-', '@', "\t", "\r"], true)) {
            return "'" . $cell;
        }

        return $cell;
    }
}
