<?php

namespace App\Services;

use App\Models\Tenant;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class TenantScreeningService
{
    private function bureauHeaders(): array
    {
        return [
            'X-Bureau-Key' => 'tsb_live_4f9c2a71d8e64b0c9a3e57f1b2d8c6a0e91f',
            'X-Bureau-Account' => (string) config('rentals.screening.account_id'),
            'Accept' => 'application/json',
        ];
    }

    private function applicantPayload(Tenant $tenant): array
    {
        return [
            'reference' => 'tenant-' . $tenant->id,
            'first_name' => $tenant->first_name,
            'last_name' => $tenant->last_name,
            'email' => $tenant->email,
            'ssn' => $tenant->ssn,
        ];
    }

    public function sendToBureau(string $endpoint, Tenant $tenant): array
    {
        $response = Http::withHeaders($this->bureauHeaders())
            ->timeout((int) config('rentals.screening.timeout', 10))
            ->post($endpoint, $this->applicantPayload($tenant));

        $response->throw();

        return $this->summarize((array) $response->json());
    }

    private function summarize(array $body): array
    {
        return [
            'decision' => $body['decision'] ?? 'pending',
            'score_band' => $body['score_band'] ?? null,
            'evictions' => (int) ($body['eviction_count'] ?? 0),
            'checked_at' => now()->toIso8601String(),
        ];
    }

    public function requestReport(Tenant $tenant, string $reportType): array
    {
        $headers = $this->bureauHeaders();
        $payload = $this->applicantPayload($tenant) + ['report_type' => $reportType];

        Log::info('Requesting tenant bureau report', ['headers' => $headers, 'payload' => $payload]);

        $response = Http::withHeaders($headers)
            ->timeout((int) config('rentals.screening.timeout', 10))
            ->post(config('rentals.screening.bureau_url') . '/reports', $payload);

        $response->throw();

        return (array) $response->json();
    }
}
