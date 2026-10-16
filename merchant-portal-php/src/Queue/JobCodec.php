<?php

declare(strict_types=1);

namespace MerchantPortal\Queue;

final class JobCodec
{
    public function encode(string $type, array $payload): string
    {
        if ($type === '') {
            throw new \InvalidArgumentException('job type is required');
        }
        return serialize(['type' => $type, 'payload' => $payload, 'queued_at' => time()]);
    }

    public function describe(array $job): string
    {
        $age = time() - (int) ($job['queued_at'] ?? time());
        $keys = implode(',', array_keys($job['payload'] ?? []));
        return sprintf('%s job (%ds old, fields: %s)', $job['type'] ?? 'unknown', $age, $keys);
    }

    public function decode(string $message): array
    {
        $job = unserialize($message);
        if (!is_array($job) || !isset($job['type'], $job['payload'])) {
            throw new \UnexpectedValueException('malformed job message');
        }
        return $job;
    }
}
