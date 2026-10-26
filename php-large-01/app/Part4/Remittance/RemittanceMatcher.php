<?php

namespace App\Part4\Remittance;

/**
 * Decides how a received repayment is spread across a loan's open installments.
 * Policy (credit ops, 2026-03): oldest installment first; within an installment
 * the outstanding amount is settled as a whole before moving on. Anything left
 * after the last open installment is held as unapplied credit on the receipt.
 */
class RemittanceMatcher
{
    /**
     * @param  list<object>  $openInstallments  ordered oldest first
     * @return array{allocations: list<array{installment_id: int, applied_minor: int}>, unapplied_minor: int}
     */
    public function allocate(RemittanceLine $line, array $openInstallments): array
    {
        $remaining = $line->amount->minor;
        $allocations = [];

        foreach ($openInstallments as $installment) {
            if ($remaining === 0) {
                break;
            }

            $due = (int) $installment->principal_minor + (int) $installment->interest_minor;
            $outstanding = $due - (int) $installment->paid_minor;

            if ($outstanding <= 0) {
                continue;
            }

            $applied = min($remaining, $outstanding);
            $allocations[] = ['installment_id' => (int) $installment->id, 'applied_minor' => $applied];
            $remaining -= $applied;
        }

        return ['allocations' => $allocations, 'unapplied_minor' => $remaining];
    }

    /**
     * Compares the rows actually parsed per file against the counts the partner
     * declared in the manifest, returning human-readable discrepancies.
     *
     * @param  array<string, int>  $declared
     * @param  array<string, int>  $parsed
     * @return list<string>
     */
    public function manifestDiscrepancies(array $declared, array $parsed): array
    {
        $problems = [];

        foreach ($declared as $file => $expectedRows) {
            if (! array_key_exists($file, $parsed)) {
                $problems[] = "{$file}: declared in manifest but not present in bundle";
                continue;
            }

            if ($parsed[$file] !== $expectedRows) {
                $problems[] = "{$file}: manifest declares {$expectedRows} rows, parsed {$parsed[$file]}";
            }
        }

        foreach (array_diff_key($parsed, $declared) as $file => $rows) {
            $problems[] = "{$file}: present in bundle ({$rows} rows) but missing from manifest";
        }

        sort($problems);

        return $problems;
    }
}
