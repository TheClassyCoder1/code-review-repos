<?php

namespace App\Part5\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Part5\Collections\HardshipPlanCalculator;
use App\Part5\Collections\PromiseToPayService;
use App\Part5\Models\CollectionCase;
use App\Part5\Repositories\CollectionCaseRepository;
use App\Part5\Security\AgencyPortalTokenIssuer;
use DateTimeImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class CollectionCaseController extends Controller
{
    private const BUCKETS = ['1-30', '31-60', '61-90', '90+'];

    public function __construct(
        private readonly CollectionCaseRepository $cases,
        private readonly PromiseToPayService $promises,
        private readonly HardshipPlanCalculator $hardship,
        private readonly AgencyPortalTokenIssuer $portalTokens,
    ) {
    }

    public function index(Request $request): JsonResponse
    {
        $this->requireOps($request);

        $validated = $request->validate([
            'after' => ['sometimes', 'integer', 'min:0'],
            'limit' => ['sometimes', 'integer', 'min:1', 'max:200'],
            'bucket' => ['sometimes', 'array'],
            'bucket.*' => ['string', 'in:' . implode(',', self::BUCKETS)],
        ]);

        $page = $this->cases->openCasesPage(
            (int) ($validated['after'] ?? 0),
            (int) ($validated['limit'] ?? 50),
            array_values($validated['bucket'] ?? []),
        );

        return response()->json([
            'data' => $page['items']->map(fn (CollectionCase $case) => [
                'id' => $case->id,
                'loan_reference' => $case->loan_reference,
                'status' => $case->status,
                'bucket' => $case->bucket,
                'days_past_due' => $case->days_past_due,
                'arrears_cents' => $case->arrears_cents,
            ])->all(),
            'next_cursor' => $page['next_cursor'],
        ]);
    }

    public function storePromise(Request $request, int $id): JsonResponse
    {
        $agent = $this->requireOps($request);

        $validated = $request->validate([
            'amount_cents' => ['required', 'integer', 'min:1'],
            'promised_for' => ['required', 'date_format:Y-m-d'],
        ]);

        $case = $this->cases->find($id);
        if ($case === null) {
            throw new NotFoundHttpException('Collection case not found.');
        }

        $today = new DateTimeImmutable('today');
        $promise = $this->promises->record(
            $case,
            (int) $validated['amount_cents'],
            new DateTimeImmutable($validated['promised_for']),
            $agent->id,
            $today,
        );

        return response()->json([
            'id' => $promise->id,
            'case_id' => $case->id,
            'amount_cents' => $promise->amount_cents,
            'promised_for' => $promise->promised_for->format('Y-m-d'),
            'status' => $promise->status,
        ], 201);
    }

    public function hardshipQuote(Request $request, int $id): JsonResponse
    {
        $this->requireOps($request);

        $validated = $request->validate([
            'balance_cents' => ['required', 'integer', 'min:1'],
            'annual_rate_bps' => ['required', 'integer', 'min:0', 'max:5000'],
            'affordable_monthly_cents' => ['required', 'integer', 'min:1'],
            'months' => ['required', 'integer', 'min:1', 'max:12'],
        ]);

        $case = $this->cases->find($id) ?? throw new NotFoundHttpException('Collection case not found.');

        return response()->json($this->hardship->propose(
            (int) $validated['balance_cents'],
            $case->arrears_cents,
            (int) $validated['annual_rate_bps'],
            (int) $validated['affordable_monthly_cents'],
            (int) $validated['months'],
        ));
    }

    public function agencyLink(Request $request, int $id): JsonResponse
    {
        $agent = $this->requireOps($request);
        $case = $this->cases->find($id) ?? throw new NotFoundHttpException('Collection case not found.');

        return response()->json([
            'token' => $this->portalTokens->issue($agent->id, $case->id, time()),
            'expires_in' => 900,
        ]);
    }

    private function requireOps(Request $request): User
    {
        $user = $this->currentUser($request);
        if (! $user->isOps()) {
            throw new AccessDeniedHttpException('Collections endpoints are restricted to operations staff.');
        }

        return $user;
    }
}
