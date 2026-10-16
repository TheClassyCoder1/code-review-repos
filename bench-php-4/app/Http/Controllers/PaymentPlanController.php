<?php

namespace App\Http\Controllers;

use App\Http\Requests\ListInstallmentsRequest;
use App\Models\Invoice;
use App\Models\PaymentPlan;
use App\Services\PaymentPlanService;
use DomainException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Routing\Controller;

class PaymentPlanController extends Controller
{
    public function __construct(private readonly PaymentPlanService $plans)
    {
    }

    public function store(Request $request, Invoice $invoice): JsonResponse
    {
        $count = (int) $request->validate(['installments' => ['required', 'integer', 'between:2,12']])['installments'];
        abort_unless($invoice->customer_id === $request->user()->customer_id, 403);
        abort_unless($invoice->status === Invoice::STATUS_OPEN, 422, 'Only open invoices can be put on a plan.');

        try {
            $plan = $this->plans->createPlan($invoice, $count);
        } catch (DomainException $e) {
            abort(409, $e->getMessage());
        }

        return response()->json($plan->load('installments'), 201);
    }

    /**
     * A plan with its progress, for the invoice owner.
     *
     * @param  PaymentPlan  $plan  Plan to show.
     * @return JsonResponse
     */
    public function show(Request $request, PaymentPlan $plan): JsonResponse
    {
        $this->ensureOwns($request, $plan);

        return response()->json([
            'plan' => $plan,
            'invoice_number' => $plan->invoice->number,
            'remaining_cents' => $this->plans->remainingBalanceCents($plan),
            'next_due_on' => $plan->installments()->where('status', 'pending')->min('due_on'),
        ]);
    }

    /**
     * One page of a plan's installment schedule.
     *
     * @param  ListInstallmentsRequest  $request  Validated paging input.
     * @return JsonResponse
     */
    public function installments(ListInstallmentsRequest $request, PaymentPlan $plan): JsonResponse
    {
        $this->ensureOwns($request, $plan);

        $perPage = 6;
        $page = (int) $request->validated('page', 1);

        $installments = $plan->installments()
            ->orderBy('sequence')
            ->skip(($page - 1) * $perPage)
            ->take($perPage)
            ->get(['sequence', 'due_on', 'amount_cents', 'status']);

        return response()->json(['page' => $page, 'data' => $installments]);
    }

    private function ensureOwns(Request $request, PaymentPlan $plan): void
    {
        abort_unless($plan->invoice->customer_id === $request->user()->customer_id, 403);
    }
}
