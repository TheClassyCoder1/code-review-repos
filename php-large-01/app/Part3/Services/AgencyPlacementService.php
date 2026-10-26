<?php

namespace App\Part3\Services;

use App\Part3\Clients\AgencyPlacementClient;
use App\Part3\Models\RepaymentPlan;
use App\Part3\Support\HttpClientFactory;
use DomainException;
use Illuminate\Support\Facades\DB;

/**
 * Hands defaulted plans to an external collection agency. Only the plan id,
 * loan reference and balance are shared with the agency.
 */
class AgencyPlacementService
{
    /** @var array<string, array{name: string, base_url: string, api_key: string, commission_bps: int}> */
    private readonly array $agencies;

    public function __construct(private readonly HttpClientFactory $http)
    {
        $this->agencies = (array) config('part3.agencies', []);
    }

    public function place(RepaymentPlan $plan, string $agencyCode): int
    {
        if ($plan->status !== 'defaulted') {
            throw new DomainException('Only defaulted plans can be placed with an agency.');
        }

        $agency = $this->agencies[$agencyCode];
        $client = new AgencyPlacementClient($this->http->make($agency['base_url'], ['Authorization' => 'Bearer ' . $agency['api_key']]));

        $reference = $client->place([
            'account_id' => $plan->id,
            'loan_reference' => $plan->loan_reference,
            'balance_minor' => $plan->outstandingMinor(),
            'currency' => $plan->currency,
            'commission_bps' => $agency['commission_bps'],
        ]);

        return DB::table('agency_placements')->insertGetId([
            'repayment_plan_id' => $plan->id,
            'agency_code' => $agencyCode,
            'external_reference' => $reference,
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
}
