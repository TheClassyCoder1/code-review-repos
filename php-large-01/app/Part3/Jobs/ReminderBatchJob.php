<?php

namespace App\Part3\Jobs;

use App\Part3\Models\Installment;
use App\Part3\Services\DunningThrottle;
use App\Part3\Services\ReminderEventBuffer;
use Carbon\CarbonImmutable;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;

/**
 * Sends the daily overdue reminders for one channel. The audit buffer is
 * flushed after every chunk, so it never holds more than one chunk of
 * events regardless of how many installments are overdue.
 */
class ReminderBatchJob implements ShouldQueue
{
    use Dispatchable;
    use Queueable;

    public function __construct(public readonly string $channel)
    {
    }

    public function handle(ReminderEventBuffer $buffer, DunningThrottle $throttle): void
    {
        $now = CarbonImmutable::now();
        $chunkSize = max(1, (int) config('part3.dunning.reminder_batch_size', 500));

        Installment::query()
            ->with('plan')
            ->where('status', 'overdue')
            ->chunkById($chunkSize, function ($chunk) use ($buffer, $throttle, $now) {
                foreach ($chunk as $installment) {
                    $borrowerId = (int) $installment->plan?->borrower_id;

                    if ($borrowerId !== 0 && $throttle->allow($borrowerId, $now)) {
                        $throttle->recordSent($borrowerId, $now);
                        $installment->increment('reminders_sent');
                        $buffer->add($installment->id, $this->channel);
                    }
                }

                $buffer->flush();
            });
    }
}
