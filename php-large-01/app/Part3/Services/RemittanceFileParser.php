<?php

namespace App\Part3\Services;

use App\Part3\Exceptions\InvalidRemittanceRowException;

/**
 * Parses an agency remittance advice: plan_id,amount_minor,collected_on,agency_reference
 */
class RemittanceFileParser
{
    /**
     * @return array{rows: list<array{plan_id: int, amount_minor: int}>, rejected: list<string>}
     */
    public function parse(string $path): array
    {
        $contents = (string) file_get_contents($path);
        $lines = preg_split('/\r\n|\n|\r/', $contents) ?: [];
        $rows = [];
        $rejected = [];

        foreach ($lines as $index => $line) {
            if ($index === 0 || trim($line) === '') {
                continue;
            }

            try {
                $rows[] = $this->parseLine(array_map('trim', str_getcsv($line)), $index + 1);
            } catch (InvalidRemittanceRowException $e) {
                $rejected[] = $e->getMessage();
                continue;
            }
        }

        return ['rows' => $rows, 'rejected' => $rejected];
    }

    /**
     * @param list<string> $fields
     *
     * @return array{plan_id: int, amount_minor: int}
     */
    private function parseLine(array $fields, int $lineNumber): array
    {
        if (count($fields) !== 4 || ! ctype_digit($fields[0]) || ! ctype_digit($fields[1]) || (int) $fields[1] === 0) {
            throw new InvalidRemittanceRowException($lineNumber, 'expected plan id, positive amount, date and reference');
        }

        if (\DateTimeImmutable::createFromFormat('!Y-m-d', $fields[2]) === false || $fields[3] === '') {
            throw new InvalidRemittanceRowException($lineNumber, 'collected_on or agency reference is invalid');
        }

        return ['plan_id' => (int) $fields[0], 'amount_minor' => (int) $fields[1]];
    }
}
