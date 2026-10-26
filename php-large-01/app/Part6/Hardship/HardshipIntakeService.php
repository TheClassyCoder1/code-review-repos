<?php

namespace App\Part6\Hardship;

use App\Part5\Collections\CollectionCaseService;
use App\Part5\Repositories\CollectionCaseRepository;
use App\Part6\Analytics\FunnelAnalyticsClient;
use App\Part6\Support\HardshipAmount;
use App\Part6\Support\PiiMasker;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;

/**
 * Borrower-facing hardship application: validates the form, records the
 * application, and moves the open collection case into hardship so the
 * dunning cycle stops chasing while the application is reviewed.
 */
class HardshipIntakeService
{
    public function __construct(
        private readonly HardshipApplicationRepository $applications,
        private readonly CollectionCaseRepository $cases,
        private readonly CollectionCaseService $caseService,
        private readonly FunnelAnalyticsClient $analytics,
        private readonly HardshipAmount $amounts,
    ) {
    }

    /**
     * @param array<string, mixed> $input
     */
    public function submit(int $borrowerId, string $loanReference, array $input): HardshipApplication
    {
        $email = strtolower(trim((string) ($input['email'] ?? '')));
        $phone = preg_replace('/[\s()-]+/', '', (string) ($input['phone'] ?? '')) ?? '';
        $postcode = strtoupper(trim((string) ($input['postcode'] ?? '')));
        $errors = [];

        if ($email === '' || filter_var($email, FILTER_VALIDATE_EMAIL) === false) {
            $errors['email'] = 'A valid email address is required.';
        } elseif (strlen($email) > 254) {
            $errors['email'] = 'Email address is too long.';
        }

        if (preg_match('/^\+[1-9]\d{7,14}$/', $phone) !== 1) {
            $errors['phone'] = 'Phone number must be in international format, e.g. +4915112345678.';
        }

        if (preg_match('/^[A-Z0-9][A-Z0-9 -]{2,9}$/', $postcode) !== 1) {
            $errors['postcode'] = 'Postcode is not valid.';
        }

        if ($errors !== []) {
            throw ValidationException::withMessages($errors);
        }

        $reason = (string) ($input['reason'] ?? '');
        if (! in_array($reason, HardshipApplication::REASONS, true)) {
            throw ValidationException::withMessages(['reason' => 'Choose one of the listed reasons.']);
        }

        $nationalId = strtoupper(preg_replace('/\s+/', '', (string) ($input['national_id'] ?? '')) ?? '');
        if (preg_match('/^[A-Z0-9]{6,20}$/', $nationalId) !== 1) {
            throw ValidationException::withMessages(['national_id' => 'National ID is not valid.']);
        }

        $income = $this->amounts->parse((string) ($input['monthly_income'] ?? ''));
        $expenses = $this->amounts->parse((string) ($input['monthly_expenses'] ?? ''));
        if ($income < 0 || $expenses < 0) {
            throw ValidationException::withMessages(['monthly_income' => 'Amounts cannot be negative.']);
        }

        if ($this->applications->hasPendingForLoan($loanReference)) {
            throw ValidationException::withMessages(['loan_reference' => 'An application for this loan is already being reviewed.']);
        }

        $case = $this->cases->findOpenForLoan($loanReference);

        $application = DB::transaction(function () use ($borrowerId, $loanReference, $case, $reason, $income, $expenses, $nationalId, $email, $phone) {
            $application = $this->applications->create([
                'borrower_id' => $borrowerId,
                'collection_case_id' => $case?->id,
                'loan_reference' => $loanReference,
                'reason' => $reason,
                'monthly_income_cents' => $income,
                'monthly_expenses_cents' => $expenses,
                'national_id' => $nationalId,
                'email' => $email,
                'phone' => $phone,
            ]);

            if ($case !== null && $case->isOpen()) {
                $this->caseService->enterHardship($case);
            }

            return $application;
        });

        Log::info('hardship.application_submitted', ['application_id' => $application->id, 'borrower_id' => $borrowerId, 'contact' => PiiMasker::email($email)]);
        $this->analytics->stepReached($borrowerId, $application->id, 'submitted');

        return $application;
    }

    public function decide(HardshipApplication $application, bool $approve): void
    {
        if ($application->status !== HardshipApplication::STATUS_SUBMITTED) {
            return;
        }

        $application->status = $approve ? HardshipApplication::STATUS_APPROVED : HardshipApplication::STATUS_DECLINED;
        $application->decided_at = now();
        $this->applications->save($application);

        Log::info('hardship.application_decided', ['application_id' => $application->id, 'status' => $application->status]);
        $this->analytics->stepReached($application->borrower_id, $application->id, $application->status);
    }
}
