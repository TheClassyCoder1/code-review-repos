<?php

namespace App\Part7\Console;

use App\Part7\Reminders\InstalmentReminderService;
use DateTimeImmutable;
use Illuminate\Console\Command;

/**
 * Daily: remind borrowers of arrangement instalments falling due in the
 * configured number of days.
 */
class SendInstalmentReminders extends Command
{
    protected $signature = 'arrangements:remind {--date= : Run date (Y-m-d), defaults to today}';

    protected $description = 'Send reminders for repayment arrangement instalments that are due soon.';

    public function __construct(private readonly InstalmentReminderService $reminders)
    {
        parent::__construct();
    }

    public function handle(): int
    {
        $option = $this->option('date');
        $today = $option ? DateTimeImmutable::createFromFormat('!Y-m-d', (string) $option) : new DateTimeImmutable('today');

        if ($today === false) {
            $this->error('--date must be in Y-m-d format.');

            return self::INVALID;
        }

        $stats = $this->reminders->sendDue($today);
        $this->info(sprintf('Reminders sent: %d, skipped: %d, failed: %d', $stats['sent'], $stats['skipped'], $stats['failed']));

        return $stats['failed'] > 0 ? self::FAILURE : self::SUCCESS;
    }
}
