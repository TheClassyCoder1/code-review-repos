<?php

namespace App\Part3\Services;

use Illuminate\Support\Facades\DB;

/**
 * Collects "reminder sent" audit events and writes them in one multi-row
 * insert instead of one round trip per reminder.
 */
class ReminderEventBuffer
{
    /** @var list<array{installment_id: int, channel: string, sent_at: string}> */
    private array $events = [];

    public function add(int $installmentId, string $channel): void
    {
        $this->events[] = ['installment_id' => $installmentId, 'channel' => $channel, 'sent_at' => now()->toDateTimeString()];
    }

    public function flush(): int
    {
        $written = count($this->events);

        if ($written > 0) {
            DB::table('reminder_events')->insert($this->events);
            $this->events = [];
        }

        return $written;
    }
}
