<?php

namespace App\Part3\Services;

use App\Part3\Support\HttpClientFactory;
use Illuminate\Support\Facades\DB;

/**
 * Pulls the accounts an agency has handed back (recalled) since the last
 * sync and closes the matching placements. The agency API pages its
 * results with an opaque cursor.
 */
class AgencyRecallSync
{
    public function __construct(private readonly HttpClientFactory $http)
    {
    }

    public function sync(string $agencyCode, string $since): int
    {
        $agency = (array) config("part3.agencies.{$agencyCode}");
        $client = $this->http->make((string) $agency['base_url'], ['Authorization' => 'Bearer ' . $agency['api_key']]);
        $closed = 0;
        $cursor = '';

        do {
            $response = $client->get('v1/recalls', ['query' => array_filter(['since' => $since, 'cursor' => $cursor])]);
            $page = json_decode((string) $response->getBody(), true, 32, JSON_THROW_ON_ERROR);

            foreach ($page['items'] ?? [] as $item) {
                $closed += DB::table('agency_placements')
                    ->where('agency_code', $agencyCode)
                    ->where('external_reference', (string) $item['reference'])
                    ->where('status', 'placed')
                    ->update(['status' => 'recalled', 'updated_at' => now()]);
            }

            $cursor = $page['next_cursor'] ?? $cursor;
        } while ($cursor !== '');

        return $closed;
    }
}
