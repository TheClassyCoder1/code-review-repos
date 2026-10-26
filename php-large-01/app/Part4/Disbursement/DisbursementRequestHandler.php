<?php

namespace App\Part4\Disbursement;

use App\Part4\Support\BankAccountValidator;
use Illuminate\Contracts\Cache\LockTimeoutException;
use Illuminate\Support\Facades\Cache;
use RuntimeException;

/**
 * Entry point for disbursement requests (HTTP controller and the loan-service
 * consumer both come through here). Serialises work per idempotency key with
 * an atomic cache lock so that DisbursementRequestService::record() sees a
 * consistent "already recorded?" answer for concurrent retries of one request.
 */
class DisbursementRequestHandler
{
    private const LOCK_SECONDS = 30;

    private const WAIT_SECONDS = 10;

    public function __construct(
        private readonly DisbursementRequestService $service,
        private readonly BankAccountValidator $validator,
    ) {
    }

    /**
     * @param  array<string, mixed>  $payload
     * @return array{id: int, status: string, replayed: bool}
     */
    public function handle(array $payload): array
    {
        $data = DisbursementRequestData::fromPayload($payload, $this->validator);

        try {
            return Cache::store('redis')
                ->lock('disbursement-request:' . $data->idempotencyKey, self::LOCK_SECONDS)
                ->block(self::WAIT_SECONDS, fn () => $this->service->record($data));
        } catch (LockTimeoutException $e) {
            throw new RuntimeException('Disbursement request is already being processed; retry later.', 0, $e);
        }
    }
}
