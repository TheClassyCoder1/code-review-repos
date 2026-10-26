<?php

namespace App\Part6\Analytics;

use Illuminate\Http\Client\Factory as HttpFactory;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Sends hardship-funnel step events to the product analytics vendor so the
 * team can see where borrowers drop out of the application flow. Analytics
 * is best effort: a vendor outage must never fail a borrower's request.
 */
class FunnelAnalyticsClient
{
    public function __construct(
        private readonly HttpFactory $http,
        private readonly string $endpoint,
        private readonly string $writeKey,
        private readonly bool $enabled,
    ) {
    }

    /**
     * @param array<string, mixed> $properties
     */
    public function track(string $event, array $properties): void
    {
        if (! $this->enabled) {
            return;
        }

        $payload = AnalyticsPayloadFilter::idsOnly(['event' => $event] + $properties);

        try {
            $this->http
                ->withToken($this->writeKey)
                ->timeout(2)
                ->post($this->endpoint, ['events' => [$payload]]);
        } catch (Throwable $e) {
            Log::warning('hardship.analytics_failed', ['event' => $event, 'error' => $e::class]);
        }
    }

    public function stepReached(int $borrowerId, int $applicationId, string $step): void
    {
        $this->track('hardship.step', [
            'borrower_id' => $borrowerId,
            'application_id' => $applicationId,
            'step' => $step,
        ]);
    }
}
