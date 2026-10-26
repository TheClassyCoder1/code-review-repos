<?php

namespace App\Part3\Services;

use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\DB;
use InvalidArgumentException;

/**
 * Handles payment notifications from the gateway. The gateway redelivers
 * until it sees a 2xx, so the same event can arrive several times.
 */
class GatewayWebhookService
{
    public function __construct(private readonly PaymentAllocator $allocator)
    {
    }

    public function verifySignature(string $payload, string $signature): bool
    {
        return hash_equals(hash_hmac('sha256', $payload, (string) config('part3.gateway.webhook_secret')), $signature);
    }

    /**
     * @param array<string, mixed> $event
     */
    public function handle(array $event): string
    {
        $eventId = (string) ($event['id'] ?? '');
        $type = (string) ($event['type'] ?? '');

        if ($eventId === '' || $type === '') {
            throw new InvalidArgumentException('Gateway event is missing id or type.');
        }

        try {
            return DB::transaction(function () use ($event, $eventId, $type) {
                DB::table('gateway_webhook_receipts')->insert([
                    'provider' => 'gateway',
                    'event_id' => $eventId,
                    'event_type' => $type,
                    'received_at' => now(),
                ]);

                if ($type !== 'payment.succeeded') {
                    return 'ignored';
                }

                $this->allocator->allocate((int) ($event['data']['metadata']['plan_id'] ?? 0), (int) ($event['data']['amount'] ?? 0));

                return 'applied';
            });
        } catch (UniqueConstraintViolationException) {
            return 'duplicate';
        }
    }
}
