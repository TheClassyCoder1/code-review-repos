<?php

namespace App\Part3\Services;

use App\Part3\Exceptions\TransientReportingException;
use App\Part3\Support\RetryPolicy;
use Closure;

/**
 * Wraps the monthly credit-bureau payment-history submission. The bureau
 * throttles aggressively at month end, so transient rejections are retried
 * with backoff; RetryPolicy decides when to stop.
 */
class BureauReportingRetrier
{
    public function __construct(private readonly RetryPolicy $policy)
    {
    }

    public function submit(Closure $submission): mixed
    {
        $attempt = 0;

        while (true) {
            $attempt++;

            try {
                return $submission();
            } catch (TransientReportingException $e) {
                usleep($this->policy->nextDelayMs($attempt, $e) * 1000);
            }
        }
    }
}
