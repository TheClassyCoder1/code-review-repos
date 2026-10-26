<?php

namespace App\Part3\Clients;

use GuzzleHttp\Client;

/**
 * Thin wrapper over an agency's placement API. One instance per agency.
 */
class AgencyPlacementClient
{
    public function __construct(private readonly Client $client)
    {
    }

    /**
     * @param array<string, scalar|null> $account
     */
    public function place(array $account): string
    {
        $response = $this->client->post('v1/placements', ['json' => $account]);

        return (string) json_decode((string) $response->getBody(), true, 16, JSON_THROW_ON_ERROR)['reference'];
    }
}
