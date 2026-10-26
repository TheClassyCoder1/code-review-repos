<?php

namespace App\Part5\Collections;

use App\Part5\Models\CollectionCase;
use App\Part5\Repositories\CollectionCaseRepository;
use DateTimeImmutable;
use Illuminate\Support\Facades\Log;

class CollectionCaseService
{
    public function __construct(
        private readonly CollectionCaseRepository $cases,
        private readonly DelinquencyClassifier $classifier,
        private readonly WebhookSigner $signer,
    ) {
    }

    /**
     * Opens a case for a loan that has gone into arrears, or refreshes the
     * delinquency figures on the existing one. Returns null when the loan is
     * current and no case is open.
     *
     * @param list<Installment> $installments
     */
    public function syncFromSchedule(int $borrowerId, string $loanReference, array $installments, DateTimeImmutable $asOf): ?CollectionCase
    {
        $status = $this->classifier->classify($installments, $asOf);
        $case = $this->cases->findOpenForLoan($loanReference);

        if ($case === null) {
            if ($status['days_past_due'] === 0) {
                return null;
            }

            $case = $this->cases->create([
                'borrower_id' => $borrowerId,
                'loan_reference' => $loanReference,
                'status' => CollectionCase::STATUS_OPEN,
                'days_past_due' => $status['days_past_due'],
                'bucket' => $status['bucket'],
                'arrears_cents' => $status['arrears_cents'],
            ]);
            Log::info('collections.case_opened', ['case_id' => $case->id, 'bucket' => $status['bucket']]);

            return $case;
        }

        $case->days_past_due = $status['days_past_due'];
        $case->bucket = $status['bucket'];
        $case->arrears_cents = $status['arrears_cents'];
        $this->cases->save($case);

        return $case;
    }

    /**
     * Closes a case once arrears are cleared and tells the dialler to stop
     * calling. Returns true only when this call performed the close.
     */
    public function closeIfSettled(CollectionCase $case, DateTimeImmutable $now): bool
    {
        if (! $case->isOpen() || $case->arrears_cents > 0) {
            return false;
        }

        $case->status = CollectionCase::STATUS_CLOSED;
        $case->closed_at = $now;
        $this->cases->save($case);

        $payload = json_encode([
            'event' => 'case.closed',
            'case_id' => $case->id,
            'loan_reference' => $case->loan_reference,
            'closed_at' => $now->format(DATE_ATOM),
        ], JSON_THROW_ON_ERROR);

        $this->signer->dispatch('case-status', $payload);
        Log::info('collections.case_closed', ['case_id' => $case->id]);

        return true;
    }

    public function enterHardship(CollectionCase $case): void
    {
        $case->status = CollectionCase::STATUS_HARDSHIP;
        $this->cases->save($case);
        Log::info('collections.case_hardship', ['case_id' => $case->id]);
    }
}
