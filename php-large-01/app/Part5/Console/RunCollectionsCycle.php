<?php

namespace App\Part5\Console;

use App\Part5\Collections\AgencyExportWriter;
use App\Part5\Collections\ContactWindowPolicy;
use App\Part5\Collections\DunningPlanner;
use App\Part5\Collections\PromiseToPayService;
use App\Part5\Repositories\CollectionCaseRepository;
use DateTimeImmutable;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Log;

/**
 * Nightly collections cycle: settle due promises, queue the next dunning
 * notice for each open case, then write the agency hand-over file.
 */
class RunCollectionsCycle extends Command
{
    protected $signature = 'collections:cycle {--date= : Run date (Y-m-d), defaults to today} {--skip-export}';

    protected $description = 'Evaluate promises, plan dunning notices and export agency cases.';

    public function __construct(
        private readonly CollectionCaseRepository $cases,
        private readonly PromiseToPayService $promises,
        private readonly DunningPlanner $dunning,
        private readonly ContactWindowPolicy $contactWindow,
        private readonly AgencyExportWriter $agencyExport,
    ) {
        parent::__construct();
    }

    public function handle(): int
    {
        $runDate = new DateTimeImmutable($this->option('date') ?: 'today');

        $promiseResult = $this->promises->evaluateDuePromises($runDate);
        $this->info(sprintf('Promises: %d kept, %d broken.', $promiseResult['kept'], $promiseResult['broken']));

        $queued = 0;
        $deferred = 0;
        $cursor = 0;
        do {
            $page = $this->cases->openCasesPage($cursor, 200);
            foreach ($page['items'] as $case) {
                $notice = $this->dunning->nextNotice($case, $runDate);
                if ($notice === null) {
                    continue;
                }

                $sendAt = $this->contactWindow->nextAllowedAt(new DateTimeImmutable('now'), $case->timezone);
                if ($sendAt === null) {
                    $deferred++;
                    continue;
                }

                $case->last_notice = $notice;
                $case->last_notice_at = $sendAt;
                $this->cases->save($case);
                $queued++;
                Log::info('collections.notice_queued', ['case_id' => $case->id, 'notice' => $notice]);
            }
            $cursor = $page['next_cursor'];
        } while ($cursor !== null);

        $this->info(sprintf('Notices: %d queued, %d deferred.', $queued, $deferred));

        if (! $this->option('skip-export')) {
            $export = $this->agencyExport->write($runDate);
            $this->info(sprintf('Agency export: %d rows.', $export['rows']));
        }

        return self::SUCCESS;
    }
}
