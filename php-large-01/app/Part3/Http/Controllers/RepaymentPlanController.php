<?php

namespace App\Part3\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part3\Repositories\InstallmentRepository;
use App\Part3\Repositories\PlanRepository;
use App\Part3\Services\PayoffQuoteCalculator;
use App\Part3\Services\RepaymentPlanBuilder;
use Carbon\CarbonImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class RepaymentPlanController extends Controller
{
    public function __construct(
        private readonly RepaymentPlanBuilder $builder,
        private readonly PlanRepository $plans,
        private readonly InstallmentRepository $installments,
        private readonly PayoffQuoteCalculator $quotes,
    ) {
    }

    public function store(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $data = $request->validate([
            'loan_reference' => ['required', 'string', 'max:40'],
            'principal_minor' => ['required', 'integer', 'min:1'],
            'annual_rate_bps' => ['required', 'integer', 'min:0', 'max:10000'],
            'term_months' => ['required', 'integer', 'min:1'],
            'first_due_on' => ['required', 'date_format:Y-m-d', 'after:today'],
        ]);

        $plan = $this->builder->build(
            $user->id,
            (string) $data['loan_reference'],
            (int) $data['principal_minor'],
            (int) $data['annual_rate_bps'],
            (int) $data['term_months'],
            CarbonImmutable::parse((string) $data['first_due_on'])->startOfDay(),
        );

        return new JsonResponse(['id' => $plan->id, 'status' => $plan->status], 201);
    }

    public function schedule(Request $request, int $planId): JsonResponse
    {
        $plan = $this->plans->findForBorrower($planId, $this->currentUser($request)->id) ?? throw new NotFoundHttpException();

        return new JsonResponse([
            'plan_id' => $plan->id,
            'installments' => $this->installments->scheduleForPlan($plan->id)
                ->map->only(['sequence', 'due_on', 'amount_minor', 'paid_minor', 'status'])->values(),
        ]);
    }

    public function quote(Request $request, int $planId): JsonResponse
    {
        $plan = $this->plans->findForBorrower($planId, $this->currentUser($request)->id) ?? throw new NotFoundHttpException();
        $data = $request->validate(['monthly_payment_minor' => ['required', 'integer', 'min:1']]);

        return new JsonResponse($this->quotes->quote($plan->outstandingMinor(), $plan->annual_rate_bps, (int) $data['monthly_payment_minor']));
    }
}
