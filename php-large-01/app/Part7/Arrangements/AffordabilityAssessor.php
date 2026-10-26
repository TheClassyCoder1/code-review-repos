<?php

namespace App\Part7\Arrangements;

use App\Part6\Hardship\HardshipApplication;
use App\Part6\Hardship\HardshipApplicationRepository;

/**
 * Relates a proposed instalment to what the borrower told us they can
 * afford. Income figures come from the latest approved hardship application
 * for the loan, or from the proposal itself when there is none.
 */
final class AffordabilityAssessor
{
    public function __construct(
        private readonly HardshipApplicationRepository $hardship,
        private readonly int $maxIncomeShareBps,
    ) {
    }

    /**
     * @return array{disposable_income_cents: int|null, income_share_bps: int|null, source: string}
     */
    public function assess(string $loanReference, int $instalmentCents, ?int $declaredIncomeCents, ?int $declaredExpensesCents): array
    {
        $disposable = null;
        $source = 'none';

        $application = $this->hardship->latestApprovedForLoan($loanReference);
        if ($application instanceof HardshipApplication) {
            $disposable = $application->disposableIncomeCents();
            $source = 'hardship_application';
        } elseif ($declaredIncomeCents !== null && $declaredExpensesCents !== null) {
            $disposable = $declaredIncomeCents - $declaredExpensesCents;
            $source = 'declared';
        }

        return [
            'disposable_income_cents' => $disposable,
            'income_share_bps' => $disposable === null ? null : self::shareBps($instalmentCents, $disposable),
            'source' => $source,
        ];
    }

    public function maxIncomeShareBps(): int
    {
        return $this->maxIncomeShareBps;
    }

    /**
     * Largest instalment that stays within the configured share of the given
     * disposable income; zero when nothing is left over each month.
     */
    public function affordableInstalmentCents(int $disposableIncomeCents): int
    {
        if ($disposableIncomeCents <= 0) {
            return 0;
        }

        return intdiv($disposableIncomeCents * $this->maxIncomeShareBps, 10000);
    }

    /**
     * Instalment as a share of disposable income in basis points, rounded up
     * so a share just over the limit never reads as exactly on it. Zero or
     * negative disposable income is reported as the 100% ceiling and above.
     */
    public static function shareBps(int $instalmentCents, int $disposableIncomeCents): int
    {
        if ($disposableIncomeCents <= 0) {
            return 10000 + max(0, $instalmentCents);
        }

        return intdiv($instalmentCents * 10000 + $disposableIncomeCents - 1, $disposableIncomeCents);
    }
}
