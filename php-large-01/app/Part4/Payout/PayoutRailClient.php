<?php

namespace App\Part4\Payout;

use App\Part4\Support\Money;
use GuzzleHttp\ClientInterface;
use GuzzleHttp\Exception\GuzzleException;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Real-time payout API of the sponsor bank, used for instant disbursements
 * below the configured ceiling. Larger amounts go out in the bulk file.
 */
class PayoutRailClient
{
    private readonly ClientInterface $http;

    public function __construct(PayoutHttpClientFactory $factory)
    {
        $this->http = $factory->forRail();
    }

    /**
     * @return array{rail_reference: string, status: string}
     */
    public function submitTransfer(int $disbursementId, Money $amount, string $account, string $routing): array
    {
        try {
            $response = $this->http->request('POST', 'v1/transfers', [
                'json' => [
                    'client_reference' => 'DISB-' . $disbursementId,
                    'amount' => $amount->toDecimalString(),
                    'currency' => $amount->currency,
                    'beneficiary' => ['account' => $account, 'routing' => $routing],
                ],
                'headers' => ['Idempotency-Key' => 'disbursement-' . $disbursementId],
            ]);
        } catch (GuzzleException $e) {
            Log::warning('payout rail transfer failed', ['disbursement_id' => $disbursementId, 'error' => $e::class]);

            throw new RuntimeException('Payout rail is unavailable.', 0, $e);
        }

        $status = $response->getStatusCode();
        $body = json_decode((string) $response->getBody(), true) ?: [];

        if ($status !== 201 && $status !== 200) {
            Log::warning('payout rail rejected transfer', ['disbursement_id' => $disbursementId, 'status' => $status]);

            throw new RuntimeException("Payout rail rejected the transfer (HTTP {$status}).");
        }

        return [
            'rail_reference' => (string) ($body['transfer_id'] ?? ''),
            'status' => strtolower((string) ($body['status'] ?? 'pending')),
        ];
    }

    /**
     * Polls the status of a previously submitted transfer.
     */
    public function transferStatus(string $railReference): string
    {
        if (preg_match('/^[A-Za-z0-9-]{6,64}$/', $railReference) !== 1) {
            throw new RuntimeException('Malformed rail reference.');
        }

        try {
            $response = $this->http->request('GET', 'v1/transfers/' . $railReference);
        } catch (GuzzleException $e) {
            throw new RuntimeException('Payout rail is unavailable.', 0, $e);
        }

        if ($response->getStatusCode() !== 200) {
            throw new RuntimeException("Payout rail status lookup failed (HTTP {$response->getStatusCode()}).");
        }

        $body = json_decode((string) $response->getBody(), true) ?: [];

        return strtolower((string) ($body['status'] ?? 'unknown'));
    }
}
