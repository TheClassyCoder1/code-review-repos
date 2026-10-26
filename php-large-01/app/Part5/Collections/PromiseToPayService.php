<?php

namespace App\Part5\Collections;

use App\Part5\Models\CollectionCase;
use App\Part5\Models\PaymentPromise;
use App\Part5\Repositories\CollectionCaseRepository;
use DateTimeImmutable;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;

class PromiseToPayService
{
    public function __construct(
        private readonly CollectionCaseRepository $cases,
        private readonly int $maxDaysAhead,
        private readonly int $maxOpenPerCase,
    ) {
    }

    public function record(CollectionCase $case, int $amountCents, DateTimeImmutable $promisedFor, int $agentId, DateTimeImmutable $today): PaymentPromise
    {
        $errors = [];
        $days = (int) $today->setTime(0, 0)->diff($promisedFor->setTime(0, 0))->format('%r%a');

        if (! $case->isOpen()) {
            $errors['case'] = 'Promises can only be recorded on open cases.';
        }
        if ($amountCents <= 0 || $amountCents > $case->arrears_cents) {
            $errors['amount_cents'] = 'Promised amount must be positive and no more than the arrears.';
        }
        if ($days < 0 || $days > $this->maxDaysAhead) {
            $errors['promised_for'] = "Promise date must be within the next {$this->maxDaysAhead} days.";
        }
        if ($this->cases->countOpenPromises($case->id) >= $this->maxOpenPerCase) {
            $errors['case'] = 'This case already has an open promise to pay.';
        }

        if ($errors !== []) {
            throw ValidationException::withMessages($errors);
        }

        $promise = $this->cases->createPromise([
            'collection_case_id' => $case->id,
            'amount_cents' => $amountCents,
            'promised_for' => $promisedFor->format('Y-m-d'),
            'status' => PaymentPromise::STATUS_OPEN,
            'recorded_by' => $agentId,
        ]);

        Log::info('collections.promise_recorded', ['case_id' => $case->id, 'promise_id' => $promise->id]);

        return $promise;
    }

    /**
     * Settles every open promise whose date has passed: kept if the borrower
     * paid at least the promised amount since it was recorded, broken otherwise.
     *
     * @return array{kept: int, broken: int}
     */
    public function evaluateDuePromises(DateTimeImmutable $today): array
    {
        $result = ['kept' => 0, 'broken' => 0];

        foreach ($this->cases->openPromisesDueBefore($today) as $promise) {
            $case = $promise->collectionCase;
            if ($case === null) {
                continue;
            }

            $paid = $this->cases->sumPaymentsSince($case->loan_reference, $promise->created_at);
            $status = $paid >= $promise->amount_cents ? PaymentPromise::STATUS_KEPT : PaymentPromise::STATUS_BROKEN;

            $promise->status = $status;
            $promise->save();

            $result[$status]++;
            Log::info('collections.promise_evaluated', [
                'case_id' => $case->id,
                'promise_id' => $promise->id,
                'status' => $status,
            ]);
        }

        return $result;
    }
}
