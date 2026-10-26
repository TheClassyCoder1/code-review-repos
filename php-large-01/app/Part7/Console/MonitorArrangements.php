<?php

namespace App\Part7\Console;

use App\Part7\Arrangements\ArrangementRepository;
use App\Part7\Arrangements\ArrangementService;
use App\Part7\Arrangements\RepaymentArrangement;
use DateTimeImmutable;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Nightly: match posted payments against active arrangements, mark missed
 * instalments, and complete or break arrangements. Runs before the
 * collections cycle so a broken arrangement re-enters dunning the same night.
 */
class MonitorArrangements extends Command
{
    protected $signature = 'arrangements:monitor
        {--date= : Run date (Y-m-d), defaults to today}
        {--page=100 : Arrangements loaded per page}';

    protected $description = 'Evaluate active repayment arrangements against posted payments.';

    public function __construct(
        private readonly ArrangementRepository $arrangements,
        private readonly ArrangementService $service,
    ) {
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

        $graceDays = (int) config('part7.breach.grace_days', 5);
        $maxMissed = (int) config('part7.breach.max_missed', 2);
        $pageSize = max(1, (int) $this->option('page'));

        $counts = [
            RepaymentArrangement::STATUS_ACTIVE => 0,
            RepaymentArrangement::STATUS_COMPLETED => 0,
            RepaymentArrangement::STATUS_BROKEN => 0,
            'errors' => 0,
        ];
        $cursor = 0;

        do {
            $page = $this->arrangements->activePage($cursor, $pageSize);

            foreach ($page['items'] as $arrangement) {
                try {
                    $status = $this->service->evaluate($arrangement, $today, $graceDays, $maxMissed);
                    $counts[$status] = ($counts[$status] ?? 0) + 1;
                } catch (Throwable $e) {
                    $counts['errors']++;
                    Log::error('arrangements.evaluate_failed', ['arrangement_id' => $arrangement->id, 'error' => $e->getMessage()]);
                }
            }

            $cursor = $page['next_cursor'];
        } while ($cursor !== null);

        Log::info('arrangements.monitor_run', ['date' => $today->format('Y-m-d')] + $counts);
        $this->table(['still active', 'completed', 'broken', 'errors'], [[
            $counts[RepaymentArrangement::STATUS_ACTIVE],
            $counts[RepaymentArrangement::STATUS_COMPLETED],
            $counts[RepaymentArrangement::STATUS_BROKEN],
            $counts['errors'],
        ]]);

        return $counts['errors'] > 0 ? self::FAILURE : self::SUCCESS;
    }
}
