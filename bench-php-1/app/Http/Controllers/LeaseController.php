<?php

namespace App\Http\Controllers;

use App\Enums\LeaseTemplate;
use App\Http\Requests\TerminateLeaseRequest;
use App\Models\Lease;
use App\Models\Property;
use App\Models\Tenant;
use App\Repositories\LeaseRepository;
use App\Support\DocumentStore;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Validation\Rule;

class LeaseController extends Controller
{
    public function __construct(
        private readonly LeaseRepository $leases,
        private readonly DocumentStore $documents,
    ) {
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'property_id' => ['required', 'integer', 'exists:properties,id'],
            'tenant_id' => ['required', 'integer', 'exists:tenants,id'],
            'start_date' => ['required', 'date'],
            'end_date' => ['required', 'date', 'after:start_date'],
            'monthly_rent' => ['required', 'numeric', 'min:0'],
            'deposit' => ['nullable', 'numeric', 'min:0'],
        ]);

        $this->authorize('update', Property::findOrFail($data['property_id']));
        $this->authorize('update', Tenant::findOrFail($data['tenant_id']));

        $lease = Lease::create($data + [
            'landlord_id' => $request->user()->id,
            'status' => 'active',
        ]);

        return response()->json($lease, 201);
    }

    public function agreement(Request $request, Lease $lease): Response
    {
        $this->authorize('view', $lease);

        $template = LeaseTemplate::tryFrom((string) $request->query('template', 'standard')) ?? abort(422, 'Unknown template.');
        $html = $this->documents->readTemplate($template->value);

        $html = strtr($html, [
            '{{tenant}}' => e($lease->tenant->full_name),
            '{{property}}' => e($lease->property->name),
            '{{rent}}' => e(number_format((float) $lease->monthly_rent, 2)),
            '{{start}}' => e($lease->start_date->toFormattedDateString()),
        ]);

        return response($html)->header('Content-Type', 'text/html');
    }

    public function show(int $lease): JsonResponse
    {
        $record = Lease::with(['tenant', 'property', 'receipts'])->findOrFail($lease);

        return response()->json($record);
    }

    public function attachment(Request $request, Lease $lease): Response
    {
        $this->authorize('view', $lease);

        $contents = $this->documents->readAttachment($lease->id, (string) $request->query('file'));

        return response($contents)->header('Content-Type', 'application/octet-stream');
    }

    public function update(Request $request, Lease $lease): JsonResponse
    {
        $this->authorize('update', $lease);

        $data = $request->validate([
            'end_date' => ['sometimes', 'date', 'after:today'],
            'monthly_rent' => ['sometimes', 'numeric', 'min:0'],
            'notes' => ['sometimes', 'nullable', 'string', 'max:2000'],
        ]);

        $lease->fill($data)->save();

        return response()->json($lease->fresh());
    }

    public function index(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'sort' => ['nullable', Rule::in(['start_date', 'end_date', 'monthly_rent', 'created_at'])],
            'direction' => ['nullable', Rule::in(['asc', 'desc'])],
        ]);

        $rows = $this->leases->listOrdered(
            $request->user()->id,
            $validated['sort'] ?? 'start_date',
            $validated['direction'] ?? 'asc',
        );

        return response()->json(['data' => $rows]);
    }

    public function search(Request $request): JsonResponse
    {
        $request->validate(['tenant' => ['required', 'string', 'max:100']]);

        $rows = $this->leases->searchByTenantName($request->user()->id, (string) $request->query('tenant'));

        return response()->json(['data' => $rows]);
    }

    public function terminate(TerminateLeaseRequest $request, Lease $lease): JsonResponse
    {
        $lease->update([
            'status' => 'terminated',
            'end_date' => $request->date('effective_on'),
            'termination_reason' => $request->input('reason'),
        ]);

        return response()->json([
            'id' => $lease->id,
            'status' => $lease->status,
            'end_date' => $lease->end_date?->toDateString(),
        ]);
    }
}
