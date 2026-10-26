<?php

namespace App\Part3\Support;

use App\Part3\Exceptions\RetriesExhaustedException;
use Throwable;

/**
 * Capped exponential backoff. Once the configured number of attempts has
 * been used up, nextDelayMs() throws instead of returning another delay.
 */
class RetryPolicy
{
    public function __construct(
        private readonly int $maxAttempts,
        private readonly int $baseDelayMs,
        private readonly int $maxDelayMs,
    ) {
    }

    public static function forBureauReporting(): self
    {
        return new self(
            max(1, (int) config('part3.bureau_reporting.max_attempts', 5)),
            max(1, (int) config('part3.bureau_reporting.base_delay_ms', 200)),
            max(1, (int) config('part3.bureau_reporting.max_delay_ms', 5000)),
        );
    }

    /**
     * @param int $attempt 1-based number of the attempt that just failed
     */
    public function nextDelayMs(int $attempt, Throwable $lastError): int
    {
        if ($attempt >= $this->maxAttempts) {
            throw new RetriesExhaustedException("Giving up after {$attempt} attempts.", 0, $lastError);
        }

        return min($this->maxDelayMs, $this->baseDelayMs * (2 ** ($attempt - 1)) + random_int(0, $this->baseDelayMs));
    }
}
