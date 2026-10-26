<?php

namespace App\Part4\Http;

use App\Http\Controllers\Controller;
use App\Part4\Disbursement\DisbursementRequestHandler;
use App\Part4\Disbursement\DisbursementRequestService;
use App\Part4\Schedule\RepaymentScheduleService;
use DateTimeImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use InvalidArgumentException;
use RuntimeException;

class DisbursementController extends Controller
{
    public function __construct(
        private readonly DisbursementRequestHandler $handler,
        private readonly DisbursementRequestService $disbursements,
        private readonly RepaymentScheduleService $schedules,
    ) {
    }

    public function store(Request $request): JsonResponse
    {
        $this->currentUser($request);

        try {
            $result = $this->handler->handle($request->only([
                'idempotency_key', 'loan_reference', 'borrower_id', 'amount', 'currency',
                'beneficiary_account', 'beneficiary_routing', 'rail',
            ]));
        } catch (InvalidArgumentException $e) {
            return response()->json(['message' => $e->getMessage()], 422);
        } catch (RuntimeException $e) {
            return response()->json(['message' => $e->getMessage()], 409);
        }

        return response()->json([
            'id' => $result['id'],
            'status' => $result['status'],
        ], $result['replayed'] ? 200 : 201);
    }

    public function show(Request $request, int $id): JsonResponse
    {
        $this->currentUser($request);

        $disbursement = $this->disbursements->describe($id);

        if ($disbursement === null) {
            return response()->json(['message' => 'Disbursement not found.'], 404);
        }

        return response()->json($disbursement);
    }

    /**
     * Repayment summary card for the borrower portal. Accepts an optional
     * as_of date (Y-m-d) so support can see the view on a past date.
     */
    public function scheduleSummary(Request $request, int $loanId): JsonResponse
    {
        $this->currentUser($request);

        $asOfInput = (string) $request->query('as_of', '');

        if ($asOfInput === '') {
            $asOf = new DateTimeImmutable('today');
        } else {
            $asOf = DateTimeImmutable::createFromFormat('!Y-m-d', $asOfInput);

            if ($asOf === false || $asOf->format('Y-m-d') !== $asOfInput) {
                return response()->json(['message' => 'as_of must be a Y-m-d date.'], 422);
            }
        }

        $summary = $this->schedules->summarize($loanId, $asOf);

        return response()->json([
            'loan_id' => $loanId,
            'as_of' => $asOf->format('Y-m-d'),
            'next_due_on' => $summary['next_due_on'],
            'next_due_minor' => $summary['next_due_minor'],
            'overdue_minor' => $summary['overdue_minor'],
            'outstanding_principal_minor' => $summary['outstanding_principal_minor'],
        ]);
    }
}
