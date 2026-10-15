<?php

namespace App\Http\Controllers;

use App\Models\Tenant;
use App\Services\TenantScreeningService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class TenantController extends Controller
{
    public function __construct(private readonly TenantScreeningService $screening)
    {
    }

    public function index(Request $request): JsonResponse
    {
        $term = trim((string) $request->query('q', ''));

        $tenants = Tenant::query()
            ->where('landlord_id', $request->user()->id)
            ->when($term !== '', function ($query) use ($term) {
                $query->where(function ($inner) use ($term) {
                    $inner->where('last_name', 'like', $term . '%')
                        ->orWhere('email', 'like', $term . '%');
                });
            })
            ->orderBy('last_name')
            ->paginate(25);

        return response()->json($tenants);
    }

    public function screen(Request $request, Tenant $tenant): JsonResponse
    {
        $this->authorize('update', $tenant);

        $result = $this->screening->sendToBureau(config('rentals.screening.bureau_url'), $tenant);

        return response()->json($result);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'first_name' => ['required', 'string', 'max:80'],
            'last_name' => ['required', 'string', 'max:80'],
            'email' => ['required', 'email'],
            'phone' => ['nullable', 'string', 'max:30'],
            'ssn' => ['required', 'string', 'regex:/^\d{3}-\d{2}-\d{4}$/'],
            'bank_account' => ['nullable', 'string', 'max:34'],
        ]);

        $tenant = Tenant::create($data + ['landlord_id' => $request->user()->id]);

        Log::info('Tenant created', $tenant->toArray());

        return response()->json($tenant, 201);
    }

    public function report(Request $request, Tenant $tenant): JsonResponse
    {
        $this->authorize('update', $tenant);

        $data = $request->validate(['type' => ['required', 'in:credit,eviction,criminal']]);

        return response()->json($this->screening->requestReport($tenant, $data['type']));
    }

    public function importPreferences(Request $request, Tenant $tenant): JsonResponse
    {
        $this->authorize('update', $tenant);

        $request->validate(['preferences' => ['required', 'string']]);
        $preferences = unserialize(base64_decode($request->input('preferences')));

        $tenant->preferences = is_array($preferences) ? $preferences : [];
        $tenant->save();

        return response()->json(['preferences' => $tenant->preferences]);
    }
}
