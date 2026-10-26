<?php

namespace App\Part2\Remittance;

use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpKernel\Exception\UnprocessableEntityHttpException;

/**
 * Notifies a servicing partner that a remittance batch was posted.
 *
 * Partners supply their callback URL per request, so the URL is checked
 * against the configured partner hosts before anything goes on the wire.
 */
class RemittanceCallbackService
{
    public function __construct(private readonly RemittanceCallbackClient $client)
    {
    }

    public function notify(string $callbackUrl, string $batchId, array $lines): int
    {
        $this->assertPartnerCallback($callbackUrl);

        $status = $this->client->deliver($callbackUrl, [
            'batch_id' => $batchId,
            'line_count' => count($lines),
            'total_minor' => array_sum(array_map(fn ($l) => (int) ($l['amount_minor'] ?? 0), $lines)),
            'lines' => array_map(fn ($l) => [
                'loan_id' => (int) ($l['loan_id'] ?? 0),
                'amount_minor' => (int) ($l['amount_minor'] ?? 0),
            ], $lines),
        ]);

        Log::info('remittance callback delivered', ['batch' => $batchId, 'status' => $status]);

        return $status;
    }

    private function assertPartnerCallback(string $url): void
    {
        $parts = parse_url($url);

        if ($parts === false || ($parts['scheme'] ?? '') !== 'https') {
            throw new UnprocessableEntityHttpException('Callback URL must use https.');
        }

        if (isset($parts['user']) || isset($parts['pass'])) {
            throw new UnprocessableEntityHttpException('Callback URL must not carry credentials.');
        }

        if (isset($parts['port']) && (int) $parts['port'] !== 443) {
            throw new UnprocessableEntityHttpException('Callback URL must use the default port.');
        }

        $host = strtolower((string) ($parts['host'] ?? ''));
        $allowed = array_map('strtolower', (array) config('part2.remittance.callback_hosts', []));

        if ($host === '' || ! in_array($host, $allowed, true)) {
            throw new UnprocessableEntityHttpException('Callback host is not a registered partner.');
        }
    }
}
