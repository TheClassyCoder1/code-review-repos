<?php

declare(strict_types=1);

namespace MerchantPortal\Queue;

final class SignedQueue
{
    public function __construct(
        private readonly \Redis $redis,
        private readonly JobCodec $codec,
        private readonly string $secret,
    ) {
        if (strlen($secret) < 32) {
            throw new \RuntimeException('queue secret must be at least 32 bytes');
        }
    }

    public function publish(string $queue, string $type, array $payload): void
    {
        $message = $this->codec->encode($type, $payload);
        $signature = hash_hmac('sha256', $message, $this->secret);
        $this->redis->lPush($queue, $signature . '.' . $message);
    }

    public function consumeOne(string $queue, array $handlers): bool
    {
        $raw = $this->redis->rPop($queue);
        if (!is_string($raw)) {
            return false;
        }
        [$signature, $message] = explode('.', $raw, 2) + [1 => ''];
        if (!hash_equals(hash_hmac('sha256', $message, $this->secret), $signature)) {
            error_log("dropping job with bad signature from queue {$queue}");
            return true;
        }
        $job = $this->codec->decode($message);
        $handler = $handlers[$job['type']] ?? null;
        if ($handler !== null) {
            $handler($job['payload']);
        }
        return true;
    }
}
