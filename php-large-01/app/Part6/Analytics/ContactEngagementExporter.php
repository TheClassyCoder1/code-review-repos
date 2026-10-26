<?php

namespace App\Part6\Analytics;

use App\Part6\Contact\BorrowerContact;
use Illuminate\Http\Client\Factory as HttpFactory;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Pushes contact-channel engagement to the outreach optimisation vendor,
 * which scores which channel (SMS, email, call) a borrower answers best so
 * the dunning planner can pick it first.
 */
class ContactEngagementExporter
{
    public function __construct(
        private readonly HttpFactory $http,
        private readonly string $endpoint,
        private readonly string $apiKey,
    ) {
    }

    public function contactChanged(BorrowerContact $contact, string $channel): void
    {
        $profile = [
            'external_id' => (string) $contact->borrower_id,
            'full_name' => $contact->full_name,
            'email' => $contact->email,
            'phone' => $contact->phone,
            'postcode' => $contact->postcode,
            'preferred_channel' => $channel,
        ];

        try {
            $this->http
                ->withHeaders(['X-Api-Key' => $this->apiKey])
                ->timeout(3)
                ->post($this->endpoint, ['profiles' => [$profile]]);
        } catch (Throwable $e) {
            Log::warning('contact.engagement_export_failed', [
                'borrower_id' => $contact->borrower_id,
                'error' => $e::class,
            ]);
        }
    }
}
