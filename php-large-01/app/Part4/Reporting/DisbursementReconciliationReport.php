<?php

namespace App\Part4\Reporting;

use App\Part4\Support\Money;
use DateTimeImmutable;

/**
 * Daily treasury report: compares what we asked the bank to pay out against the
 * settlement lines on the bank statement and buckets the differences.
 */
class DisbursementReconciliationReport
{
    /**
     * Matches requested disbursements to bank settlement lines by rail reference
     * and classifies each pair.
     *
     * @param  list<array{rail_reference: string, amount_minor: int, currency: string}>  $requested
     * @param  list<array{rail_reference: string, amount_minor: int, currency: string}>  $settled
     * @return array{matched: list<string>, amount_mismatch: list<string>, missing_on_statement: list<string>, unexpected_on_statement: list<string>}
     */
    public function reconcile(array $requested, array $settled): array
    {
        $result = ['matched' => [], 'amount_mismatch' => [], 'missing_on_statement' => [], 'unexpected_on_statement' => []];
        $settledByRef = [];

        foreach ($settled as $line) {
            $settledByRef[$line['rail_reference']] = $line;
        }

        foreach ($requested as $line) {
            $reference = $line['rail_reference'];
            $bank = $settledByRef[$reference] ?? null;

            if ($bank === null) {
                $result['missing_on_statement'][] = $reference;
                continue;
            }

            unset($settledByRef[$reference]);

            $sameAmount = $bank['currency'] === $line['currency'] && $bank['amount_minor'] === $line['amount_minor'];
            $result[$sameAmount ? 'matched' : 'amount_mismatch'][] = $reference;
        }

        $result['unexpected_on_statement'] = array_keys($settledByRef);

        return $result;
    }

    /**
     * Totals per currency, so the report header can show one control total per
     * settlement account instead of adding rupees to euros.
     *
     * @param  list<array{amount_minor: int, currency: string}>  $lines
     * @return array<string, string> currency => decimal total
     */
    public function totalsByCurrency(array $lines): array
    {
        $totals = [];

        foreach ($lines as $line) {
            $amount = Money::ofMinor($line['amount_minor'], $line['currency']);
            $totals[$amount->currency] = isset($totals[$amount->currency])
                ? $totals[$amount->currency]->plus($amount)
                : $amount;
        }

        ksort($totals);

        $formatted = [];

        foreach ($totals as $currency => $total) {
            $formatted[$currency] = $total->toDecimalString();
        }

        return $formatted;
    }

    /**
     * Groups unresolved disbursements by how many business days they have been
     * outstanding (weekends skipped), for the ageing table at the bottom of the report.
     *
     * @param  array<string, DateTimeImmutable>  $submittedOn  rail reference => submission date
     * @return array{'0-1': list<string>, '2-3': list<string>, '4+': list<string>}
     */
    public function ageingBuckets(array $submittedOn, DateTimeImmutable $asOf): array
    {
        $buckets = ['0-1' => [], '2-3' => [], '4+' => []];
        $asOfDay = $asOf->setTime(0, 0);

        foreach ($submittedOn as $reference => $date) {
            $cursor = $date->setTime(0, 0);
            $businessDays = 0;

            while ($cursor < $asOfDay) {
                $cursor = $cursor->modify('+1 day');
                if ((int) $cursor->format('N') <= 5) {
                    $businessDays++;
                }
            }

            $bucket = match (true) {
                $businessDays <= 1 => '0-1',
                $businessDays <= 3 => '2-3',
                default => '4+',
            };

            $buckets[$bucket][] = (string) $reference;
        }

        return $buckets;
    }
}
