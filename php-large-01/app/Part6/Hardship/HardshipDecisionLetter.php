<?php

namespace App\Part6\Hardship;

use App\Part6\Support\BorrowerDates;
use App\Part6\Support\HardshipAmount;
use DateTimeImmutable;

/**
 * Plain-text body of the letter sent once a hardship application is decided.
 * Wording is approved by compliance; change it only together with them.
 */
class HardshipDecisionLetter
{
    public function __construct(
        private readonly ReliefCalculator $relief,
        private readonly HardshipAmount $amounts,
        private readonly int $feeReliefBps,
        private readonly int $interestReliefBps,
    ) {
    }

    public function render(HardshipApplication $application, int $accruedFeesCents, int $accruedInterestCents, DateTimeImmutable $sentOn, ?string $timezone): string
    {
        $lines = [
            BorrowerDates::forLetter($sentOn, $timezone),
            '',
            "Loan reference: {$application->loan_reference}",
            '',
        ];

        if ($application->status === HardshipApplication::STATUS_APPROVED) {
            $relief = $this->relief->reliefFor($accruedFeesCents, $accruedInterestCents, $this->feeReliefBps, $this->interestReliefBps);
            $lines[] = 'We have approved your request for hardship support.';
            $lines[] = sprintf('Fees waived: %s', $this->amounts->format($relief['fee_relief_cents']));
            $lines[] = sprintf('Interest waived: %s', $this->amounts->format($relief['interest_relief_cents']));
            if ($relief['capped']) {
                $lines[] = 'This is the maximum relief available under our hardship policy.';
            }
        } else {
            $lines[] = 'We are unable to approve your request for hardship support at this time.';
            $lines[] = 'You can ask us to review this decision within 30 days of the date above.';
        }

        if ($application->decided_at !== null) {
            $lines[] = '';
            $lines[] = 'Decision date: ' . BorrowerDates::forDisplay($application->decided_at, $timezone);
        }

        return implode("\n", $lines) . "\n";
    }
}
