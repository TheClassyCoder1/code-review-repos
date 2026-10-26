<?php

namespace App\Part4\Schedule;

use DateTimeImmutable;
use Illuminate\Support\Facades\Log;
use RuntimeException;

class RepaymentScheduleService
{
    public function __construct(
        private readonly RepaymentScheduleRepository $repository,
        private readonly AmortizationCalculator $calculator,
    ) {
    }

    /**
     * Rebuilds the stored schedule when the loan's terms have moved past the
     * version the schedule was generated from (rate change, restructure, top-up).
     *
     * Runs from ScheduleRegenerationJob.
     *
     * @return bool true when a new schedule was written
     */
    public function regenerateIfStale(int $loanId): bool
    {
        $loan = $this->repository->findLoanTerms($loanId);

        if ($loan === null) {
            throw new RuntimeException("Loan {$loanId} not found.");
        }

        $currentVersion = $this->repository->currentScheduleVersion($loanId);

        if ($currentVersion !== null && $currentVersion >= (int) $loan->terms_version) {
            return false;
        }

        $installments = $this->calculator->equalInstallmentSchedule(
            (int) $loan->principal_minor,
            (int) $loan->annual_rate_bps,
            (int) $loan->term_months,
            new DateTimeImmutable((string) $loan->first_due_on),
        );

        $this->repository->replace($loanId, (int) $loan->terms_version, $installments);

        Log::info('repayment schedule regenerated', [
            'loan_id' => $loanId,
            'terms_version' => (int) $loan->terms_version,
            'installments' => count($installments),
        ]);

        return true;
    }

    /**
     * Portal summary card: next due installment, overdue total and remaining
     * principal as of the given date.
     *
     * @return array{next_due_on: ?string, next_due_minor: int, overdue_minor: int, outstanding_principal_minor: int}
     */
    public function summarize(int $loanId, DateTimeImmutable $asOf): array
    {
        $nextDueOn = null;
        $nextDueMinor = 0;
        $overdueMinor = 0;
        $outstandingPrincipal = 0;
        $today = $asOf->format('Y-m-d');

        foreach ($this->repository->installments($loanId) as $row) {
            $due = (int) $row->principal_minor + (int) $row->interest_minor;
            $unpaid = max(0, $due - (int) $row->paid_minor);

            if ($unpaid === 0) {
                continue;
            }

            $principalShare = $due === 0 ? 0 : intdiv((int) $row->principal_minor * $unpaid, $due);
            $outstandingPrincipal += $principalShare;

            if ((string) $row->due_on < $today) {
                $overdueMinor += $unpaid;
            } elseif ($nextDueOn === null) {
                $nextDueOn = (string) $row->due_on;
                $nextDueMinor = $unpaid;
            }
        }

        return [
            'next_due_on' => $nextDueOn,
            'next_due_minor' => $nextDueMinor,
            'overdue_minor' => $overdueMinor,
            'outstanding_principal_minor' => $outstandingPrincipal,
        ];
    }
}
