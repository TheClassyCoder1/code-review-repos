<?php

namespace App\Part4\Schedule;

use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldBeUnique;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\Middleware\WithoutOverlapping;

/**
 * Queued after any change to a loan's terms. Only one regeneration per loan may
 * be queued (ShouldBeUnique) and only one may execute at a time
 * (WithoutOverlapping, keyed on the loan id), so the version check and the
 * schedule replace inside RepaymentScheduleService never interleave for the
 * same loan.
 */
class ScheduleRegenerationJob implements ShouldQueue, ShouldBeUnique
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;

    public int $tries = 5;

    public int $uniqueFor = 600;

    public function __construct(public readonly int $loanId)
    {
        $this->onQueue('schedules');
    }

    public function uniqueId(): string
    {
        return 'loan-schedule:' . $this->loanId;
    }

    /**
     * @return list<object>
     */
    public function middleware(): array
    {
        return [
            (new WithoutOverlapping('loan-schedule:' . $this->loanId))
                ->releaseAfter(15)
                ->expireAfter(300),
        ];
    }

    public function handle(RepaymentScheduleService $schedules): void
    {
        $schedules->regenerateIfStale($this->loanId);
    }
}
