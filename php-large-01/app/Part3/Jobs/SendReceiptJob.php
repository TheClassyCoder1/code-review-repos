<?php

namespace App\Part3\Jobs;

use App\Part3\Repositories\PlanRepository;
use App\Part3\Services\ReceiptMailer;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Support\Facades\DB;
use RuntimeException;

/**
 * Queued after every applied payment. A failed delivery is released back
 * onto the queue with a growing delay and, once the attempts are used up,
 * fails the job so it lands in failed_jobs for the support team.
 */
class SendReceiptJob implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;

    public int $tries = 5;

    public function __construct(public readonly int $planId, public readonly int $amountMinor)
    {
    }

    public function handle(ReceiptMailer $mailer, PlanRepository $plans): void
    {
        $plan = $plans->find($this->planId);
        $recipient = $plan === null ? null : DB::table('users')->where('id', $plan->borrower_id)->value('email');

        if ($plan === null || ! is_string($recipient) || $mailer->send($plan, $recipient, $this->amountMinor)) {
            return;
        }

        if ($this->attempts() >= $this->tries) {
            $this->fail(new RuntimeException("Receipt for plan {$this->planId} could not be delivered."));

            return;
        }

        $this->release(60 * $this->attempts());
    }
}
