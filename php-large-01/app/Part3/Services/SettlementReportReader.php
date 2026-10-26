<?php

namespace App\Part3\Services;

use Illuminate\Support\Facades\Log;
use JsonException;
use RuntimeException;

/**
 * Reads the gateway's daily settlement report (JSON lines): every payment
 * the gateway has actually paid out to us.
 */
class SettlementReportReader
{
    /**
     * @param resource $stream
     *
     * @return list<array{payment_reference: string, amount_minor: int}>
     */
    public function read($stream, string $reportId): array
    {
        $entries = [];
        $lineNumber = 0;

        try {
            while (($line = fgets($stream)) !== false) {
                $lineNumber++;
                $row = json_decode($line, true, 8, JSON_THROW_ON_ERROR);

                if (! isset($row['reference'], $row['amount'])) {
                    throw new RuntimeException("Settlement line {$lineNumber} is missing fields.");
                }

                $entries[] = ['payment_reference' => (string) $row['reference'], 'amount_minor' => (int) $row['amount']];
            }
        } catch (JsonException $e) {
            throw new RuntimeException("Settlement line {$lineNumber} is not valid JSON.", 0, $e);
        } finally {
            Log::info('settlement report read', ['report_id' => $reportId, 'lines' => $lineNumber]);

            return $entries;
        }
    }
}
