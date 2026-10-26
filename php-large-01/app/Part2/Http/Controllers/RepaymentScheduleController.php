<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\Collections\RepaymentScheduleRepository;
use App\Part2\Collections\RestructuringQuoteService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class RepaymentScheduleController extends Controller
{
    public function __construct(
        private readonly RepaymentScheduleRepository $schedules,
        private readonly RestructuringQuoteService $quotes,
    ) {
    }

    public function show(Request $request, int $loanId): JsonResponse
    {
        $instalments = $this->schedules->forLoan($loanId);

        return new JsonResponse([
            'loanId' => $loanId,
            'instalments' => array_map(fn ($i) => [
                'sequence' => (int) $i->sequence,
                'dueOn' => $i->due_on,
                'principalCents' => (int) $i->principal_cents,
                'interestCents' => (int) $i->interest_cents,
                'status' => $i->status,
            ], $instalments),
        ]);
    }

    public function quote(Request $request, int $loanId): JsonResponse
    {
        $validated = $request->validate([
            'plan' => ['required', 'string', 'max:24'],
        ]);

        return new JsonResponse(['loanId' => $loanId] + $this->quotes->quote($loanId, $validated['plan']));
    }
}
