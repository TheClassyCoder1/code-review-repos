<?php

namespace App\Part7\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part6\Hardship\LoanReferenceLookup;
use App\Part6\Support\BorrowerDates;
use App\Part6\Support\HardshipAmount;
use App\Part7\Arrangements\ArrangementInstalment;
use App\Part7\Arrangements\ArrangementRepository;
use App\Part7\Arrangements\ArrangementService;
use App\Part7\Arrangements\RepaymentArrangement;
use DateTimeImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use InvalidArgumentException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class RepaymentArrangementController extends Controller
{
    public function __construct(
        private readonly ArrangementService $service,
        private readonly ArrangementRepository $arrangements,
        private readonly LoanReferenceLookup $loans,
        private readonly HardshipAmount $amounts,
    ) {
    }

    /**
     * POST /api/v1/loans/{loanId}/arrangements/quote
     * Preview a schedule without saving anything.
     */
    public function quote(Request $request, int $loanId): JsonResponse
    {
        $validated = $this->validateTerms($request);
        $reference = $this->referenceFor($loanId);

        $disposable = null;
        if (isset($validated['monthly_income'], $validated['monthly_expenses'])) {
            $disposable = $this->cents($validated['monthly_income'], 'monthly_income')
                - $this->cents($validated['monthly_expenses'], 'monthly_expenses');
        }

        $quote = $this->service->quote(
            $reference,
            (int) $validated['instalments'],
            $this->date($validated['first_due_on']),
            new DateTimeImmutable('today'),
            $disposable,
        );

        return response()->json([
            'instalment_count' => $quote['instalment_count'],
            'instalment_amount' => $this->amounts->format($quote['instalment_cents']),
            'suggested_instalment_count' => $quote['suggested_count'],
            'schedule' => array_map(fn (array $row) => [
                'sequence' => $row['sequence'],
                'due_on' => $row['due_on'],
                'amount' => $this->amounts->format($row['amount_cents']),
            ], $quote['schedule']),
        ]);
    }

    /** POST /api/v1/loans/{loanId}/arrangements */
    public function store(Request $request, int $loanId): JsonResponse
    {
        $user = $this->currentUser($request);
        $validated = $this->validateTerms($request);
        $reference = $this->referenceFor($loanId);

        $income = isset($validated['monthly_income']) ? $this->cents($validated['monthly_income'], 'monthly_income') : null;
        $expenses = isset($validated['monthly_expenses']) ? $this->cents($validated['monthly_expenses'], 'monthly_expenses') : null;

        $arrangement = $this->service->propose(
            $user->id,
            $reference,
            (int) $validated['instalments'],
            $this->date($validated['first_due_on']),
            new DateTimeImmutable('today'),
            $income,
            $expenses,
        );

        return response()->json($this->present($arrangement), 201);
    }

    /** GET /api/v1/arrangements/{id} */
    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json($this->present($this->visibleArrangement($request, $id)));
    }

    /** POST /api/v1/arrangements/{id}/accept (borrower only) */
    public function accept(Request $request, int $id): JsonResponse
    {
        $arrangement = $this->ownedArrangement($request, $id);
        $this->service->accept($arrangement, new DateTimeImmutable());

        return response()->json($this->present($arrangement));
    }

    /** POST /api/v1/arrangements/{id}/cancel (borrower or operations staff) */
    public function cancel(Request $request, int $id): JsonResponse
    {
        $user = $this->currentUser($request);
        $arrangement = $this->visibleArrangement($request, $id);
        $reason = $user->isOps() ? RepaymentArrangement::END_OPS : RepaymentArrangement::END_BORROWER;

        $this->service->cancel($arrangement, $reason, new DateTimeImmutable());

        return response()->json($this->present($arrangement->refresh()->load('instalments')));
    }

    /** PUT /api/v1/arrangements/{id}/reminders */
    public function reminders(Request $request, int $id): JsonResponse
    {
        $validated = $request->validate(['enabled' => ['required', 'boolean']]);
        $arrangement = $this->ownedArrangement($request, $id);

        $this->service->setRemindersOptedOut($arrangement, ! (bool) $validated['enabled']);

        return response()->json(['id' => $arrangement->id, 'reminders_enabled' => ! $arrangement->reminders_opted_out]);
    }

    /**
     * @return array{instalments: int|string, first_due_on: string, monthly_income?: string, monthly_expenses?: string}
     */
    private function validateTerms(Request $request): array
    {
        return $request->validate([
            'instalments' => ['required', 'integer'],
            'first_due_on' => ['required', 'date_format:Y-m-d'],
            'monthly_income' => ['sometimes', 'string', 'max:20'],
            'monthly_expenses' => ['sometimes', 'string', 'max:20'],
        ]);
    }

    private function referenceFor(int $loanId): string
    {
        return $this->loans->referenceFor($loanId) ?? throw new NotFoundHttpException('Loan not found.');
    }

    private function visibleArrangement(Request $request, int $id): RepaymentArrangement
    {
        $user = $this->currentUser($request);
        $arrangement = $this->arrangements->findWithInstalments($id);

        if ($arrangement === null || (! $user->isOps() && $arrangement->borrower_id !== $user->id)) {
            throw new NotFoundHttpException('Arrangement not found.');
        }

        return $arrangement;
    }

    private function ownedArrangement(Request $request, int $id): RepaymentArrangement
    {
        $user = $this->currentUser($request);
        $arrangement = $this->arrangements->findWithInstalments($id);

        if ($arrangement === null || $arrangement->borrower_id !== $user->id) {
            throw new NotFoundHttpException('Arrangement not found.');
        }

        return $arrangement;
    }

    private function cents(string $decimal, string $field): int
    {
        try {
            $cents = $this->amounts->parse($decimal);
        } catch (InvalidArgumentException) {
            throw ValidationException::withMessages([$field => 'Enter an amount such as 1250.00.']);
        }

        if ($cents < 0) {
            throw ValidationException::withMessages([$field => 'Amounts cannot be negative.']);
        }

        return $cents;
    }

    private function date(string $value): DateTimeImmutable
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);

        if ($date === false) {
            throw ValidationException::withMessages(['first_due_on' => 'Use the format YYYY-MM-DD.']);
        }

        return $date;
    }

    /**
     * @return array<string, mixed>
     */
    private function present(RepaymentArrangement $arrangement): array
    {
        return [
            'id' => $arrangement->id,
            'loan_reference' => $arrangement->loan_reference,
            'status' => $arrangement->status,
            'total' => $this->amounts->format($arrangement->total_cents),
            'instalment_count' => $arrangement->instalment_count,
            'instalment_amount' => $this->amounts->format($arrangement->instalment_cents),
            'missed_count' => $arrangement->missed_count,
            'reminders_enabled' => ! $arrangement->reminders_opted_out,
            'accepted_on' => $arrangement->accepted_at ? BorrowerDates::iso($arrangement->accepted_at) : null,
            'ended_on' => $arrangement->ended_at ? BorrowerDates::iso($arrangement->ended_at) : null,
            'end_reason' => $arrangement->end_reason,
            'instalments' => $arrangement->instalments->map(fn (ArrangementInstalment $instalment) => [
                'sequence' => $instalment->sequence,
                'due_on' => BorrowerDates::iso($instalment->due_on),
                'amount' => $this->amounts->format($instalment->amount_cents),
                'paid' => $this->amounts->format($instalment->paid_cents),
                'status' => $instalment->status,
            ])->values()->all(),
        ];
    }
}
