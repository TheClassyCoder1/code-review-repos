<?php

namespace App\Part3\Services;

use Carbon\CarbonImmutable;
use Illuminate\Contracts\Cache\Repository as CacheRepository;

/**
 * Consumer-protection cap on payment reminders: a borrower may receive at
 * most N reminders per ISO week across SMS, e-mail and push. Reminder
 * workers run in parallel, one per channel.
 */
class DunningThrottle
{
    public function __construct(private readonly CacheRepository $cache)
    {
    }

    public function allow(int $borrowerId, CarbonImmutable $now): bool
    {
        return (int) $this->cache->get($this->key($borrowerId, $now), 0) < (int) config('part3.dunning.max_reminders_per_week', 3);
    }

    public function recordSent(int $borrowerId, CarbonImmutable $now): int
    {
        $key = $this->key($borrowerId, $now);

        $sent = (int) $this->cache->get($key, 0);
        $sent++;
        $this->cache->put($key, $sent, $now->endOfWeek()->addDay());

        return $sent;
    }

    private function key(int $borrowerId, CarbonImmutable $now): string
    {
        return sprintf('dunning:%d:%s', $borrowerId, $now->format('o-\WW'));
    }
}
