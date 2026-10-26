<?php

namespace App\Part3\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part3\Http\Requests\AgencyPlacementRequest;
use App\Part3\Http\Requests\RemittanceUploadRequest;
use App\Part3\Repositories\PlanRepository;
use App\Part3\Services\AgencyPlacementService;
use App\Part3\Services\RemittanceImportService;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class AgencyController extends Controller
{
    public function __construct(
        private readonly PlanRepository $plans,
        private readonly AgencyPlacementService $placements,
        private readonly RemittanceImportService $remittances,
    ) {
    }

    public function place(AgencyPlacementRequest $request): JsonResponse
    {
        $plan = $this->plans->find((int) $request->validated('plan_id')) ?? throw new NotFoundHttpException();

        return new JsonResponse(['placement_id' => $this->placements->place($plan, $request->agencyCode())], 201);
    }

    public function remittance(RemittanceUploadRequest $request): JsonResponse
    {
        $applied = $this->remittances->import($request->file('remittance')->getRealPath());

        return new JsonResponse(['agency_code' => $request->validated('agency_code'), 'applied_minor' => $applied], 201);
    }
}
