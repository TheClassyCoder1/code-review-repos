<?php

namespace App\Part7\Arrangements;

use App\Part5\Collections\CollectionCaseService;
use App\Part5\Models\CollectionCase;
use App\Part5\Repositories\CollectionCaseRepository;
use DateTimeImmutable;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;
use InvalidArgumentException;

/**
 * Lifecycle of a repayment arrangement: a borrower proposes a schedule for
 * the arrears on their open collection case, accepts it, pays instalments,
 * and the arrangement either completes, is cancelled, or breaks after too
 * many missed instalments.
 */
class ArrangementService
{
    public function __construct(
        private readonly ArrangementRepository $arrangements,
        private readonly CollectionCaseRepository $cases,
        private readonly CollectionCaseService $caseService,
        private readonly ArrangementTermsCalculator $terms,
        private readonly AffordabilityAssessor $affordability,
    ) {
    }

    /**
     * @return array{instalment_count: int, instalment_cents: int, schedule: list<array{sequence: int, due_on: string, amount_cents: int}>, suggested_count: int|null}
     */
    public function quote(string $loanReference, int $count, DateTimeImmutable $firstDue, DateTimeImmutable $today, ?int $disposableIncomeCents = null): array
    {
        $case = $this->openCaseFor($loanReference);
        $quote = $this->computeTerms($case, $count, $firstDue, $today);

        $suggested = null;
        if ($disposableIncomeCents !== null) {
            $target = $this->affordability->affordableInstalmentCents($disposableIncomeCents);
            $suggested = $target > 0 ? $this->terms->countForTarget($case->arrears_cents, $target) : null;
        }

        return $quote + ['suggested_count' => $suggested];
    }

    public function propose(
        int $borrowerId,
        string $loanReference,
        int $count,
        DateTimeImmutable $firstDue,
        DateTimeImmutable $today,
        ?int $declaredIncomeCents,
        ?int $declaredExpensesCents,
    ): RepaymentArrangement {
        $case = $this->openCaseFor($loanReference);

        if ($case->borrower_id !== $borrowerId) {
            throw ValidationException::withMessages(['loan_reference' => 'This collection case belongs to another borrower.']);
        }

        if ($this->arrangements->liveForLoan($loanReference) !== null) {
            throw ValidationException::withMessages(['loan_reference' => 'This loan already has a repayment arrangement in progress.']);
        }

        $quote = $this->computeTerms($case, $count, $firstDue, $today);
        $affordability = $this->affordability->assess(
            $loanReference,
            $quote['instalment_cents'],
            $declaredIncomeCents,
            $declaredExpensesCents,
        );

        $arrangement = $this->arrangements->createWithInstalments([
            'borrower_id' => $borrowerId,
            'collection_case_id' => $case->id,
            'loan_reference' => $loanReference,
            'total_cents' => $case->arrears_cents,
            'instalment_count' => $quote['instalment_count'],
            'instalment_cents' => $quote['instalment_cents'],
            'disposable_income_cents' => $affordability['disposable_income_cents'],
            'income_share_bps' => $affordability['income_share_bps'],
            'status' => RepaymentArrangement::STATUS_PROPOSED,
            'missed_count' => 0,
            'reminders_opted_out' => false,
            'timezone' => $case->timezone,
        ], $quote['schedule']);

        Log::info('arrangements.proposed', [
            'arrangement_id' => $arrangement->id,
            'case_id' => $case->id,
            'instalments' => $arrangement->instalment_count,
            'affordability_source' => $affordability['source'],
        ]);

        return $arrangement;
    }

    public function accept(RepaymentArrangement $arrangement, DateTimeImmutable $now): void
    {
        if ($arrangement->status !== RepaymentArrangement::STATUS_PROPOSED) {
            throw ValidationException::withMessages(['status' => 'Only a proposed arrangement can be accepted.']);
        }

        $first = $arrangement->instalments->first();
        if ($first !== null && $first->due_on->toDateString() <= $now->format('Y-m-d')) {
            throw ValidationException::withMessages(['status' => 'This proposal has expired; please request a new schedule.']);
        }

        DB::transaction(function () use ($arrangement, $now) {
            $arrangement->status = RepaymentArrangement::STATUS_ACTIVE;
            $arrangement->accepted_at = $now;
            $this->arrangements->save($arrangement);

            $case = $this->cases->find($arrangement->collection_case_id);
            if ($case !== null && $case->status === CollectionCase::STATUS_OPEN) {
                // Dunning stops while the borrower is paying to the agreed schedule.
                $this->caseService->enterHardship($case);
            }
        });

        Log::info('arrangements.accepted', ['arrangement_id' => $arrangement->id]);
    }

    public function cancel(RepaymentArrangement $arrangement, string $reason, DateTimeImmutable $now): void
    {
        if (! $arrangement->isLive()) {
            return;
        }

        $this->end($arrangement, RepaymentArrangement::STATUS_CANCELLED, $reason, $now);
    }

    public function setRemindersOptedOut(RepaymentArrangement $arrangement, bool $optedOut): void
    {
        $arrangement->reminders_opted_out = $optedOut;
        $this->arrangements->save($arrangement);

        Log::info('arrangements.reminder_preference', ['arrangement_id' => $arrangement->id, 'opted_out' => $optedOut]);
    }

    /**
     * Applies payments posted since acceptance to the instalments in due-date
     * order, marks overdue instalments as missed once the grace period has
     * passed, and completes or breaks the arrangement accordingly.
     *
     * @return string the arrangement status after evaluation
     */
    public function evaluate(RepaymentArrangement $arrangement, DateTimeImmutable $today, int $graceDays, int $maxMissed): string
    {
        if (! $arrangement->isActive() || $arrangement->accepted_at === null) {
            return $arrangement->status;
        }

        $paid = $this->arrangements->sumPaymentsSince($arrangement->loan_reference, $arrangement->accepted_at->toDateTimeImmutable());
        $remaining = $paid;
        $missed = 0;
        $graceCutoff = $today->setTime(0, 0)->modify("-{$graceDays} days")->format('Y-m-d');

        foreach ($arrangement->instalments as $instalment) {
            if ($instalment->status === ArrangementInstalment::STATUS_VOID) {
                continue;
            }

            $applied = min($remaining, $instalment->amount_cents);
            $remaining -= $applied;
            $instalment->paid_cents = $applied;

            if ($instalment->outstandingCents() === 0) {
                if ($instalment->status !== ArrangementInstalment::STATUS_PAID) {
                    $instalment->status = ArrangementInstalment::STATUS_PAID;
                    $instalment->settled_at = $today;
                }
            } elseif ($instalment->due_on->toDateString() < $graceCutoff) {
                $instalment->status = ArrangementInstalment::STATUS_MISSED;
                $missed++;
            }

            if ($instalment->isDirty()) {
                $this->arrangements->saveInstalment($instalment);
            }
        }

        $arrangement->missed_count = $missed;

        if ($paid >= $arrangement->total_cents) {
            $this->end($arrangement, RepaymentArrangement::STATUS_COMPLETED, RepaymentArrangement::END_PAID, $today);
        } elseif ($missed > $maxMissed) {
            $this->end($arrangement, RepaymentArrangement::STATUS_BROKEN, RepaymentArrangement::END_MISSED, $today);
        } else {
            $this->arrangements->save($arrangement);
        }

        return $arrangement->status;
    }

    private function end(RepaymentArrangement $arrangement, string $status, string $reason, DateTimeImmutable $now): void
    {
        DB::transaction(function () use ($arrangement, $status, $reason, $now) {
            $arrangement->status = $status;
            $arrangement->end_reason = $reason;
            $arrangement->ended_at = $now;
            $this->arrangements->save($arrangement);
            $this->arrangements->voidOpenInstalments($arrangement);

            $case = $this->cases->find($arrangement->collection_case_id);
            if ($case !== null && $case->status === CollectionCase::STATUS_HARDSHIP && $status !== RepaymentArrangement::STATUS_COMPLETED) {
                // Back into the normal dunning cycle.
                $case->status = CollectionCase::STATUS_OPEN;
                $this->cases->save($case);
            } elseif ($case !== null && $status === RepaymentArrangement::STATUS_COMPLETED) {
                $this->caseService->closeIfSettled($case, $now);
            }
        });

        Log::info('arrangements.ended', ['arrangement_id' => $arrangement->id, 'status' => $status, 'reason' => $reason]);
    }

    private function openCaseFor(string $loanReference): CollectionCase
    {
        $case = $this->cases->findOpenForLoan($loanReference);

        if ($case === null || $case->arrears_cents <= 0) {
            throw ValidationException::withMessages(['loan_reference' => 'There are no arrears on this loan to arrange.']);
        }

        return $case;
    }

    /**
     * @return array{instalment_count: int, instalment_cents: int, schedule: list<array{sequence: int, due_on: string, amount_cents: int}>}
     */
    private function computeTerms(CollectionCase $case, int $count, DateTimeImmutable $firstDue, DateTimeImmutable $today): array
    {
        try {
            return $this->terms->quote($case->arrears_cents, $count, $firstDue, $today);
        } catch (InvalidArgumentException $e) {
            throw ValidationException::withMessages(['instalments' => $e->getMessage()]);
        }
    }
}
