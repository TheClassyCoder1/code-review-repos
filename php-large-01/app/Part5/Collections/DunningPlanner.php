<?php

namespace App\Part5\Collections;

use App\Part5\Models\CollectionCase;
use DateTimeImmutable;

/**
 * Decides which collections notice (if any) is due for a case today.
 * Notices are escalated in configured order and never skipped backwards.
 */
class DunningPlanner
{
    /**
     * @param array<string, int> $steps notice => days past due at which it becomes due
     */
    public function __construct(
        private readonly array $steps,
        private readonly int $minDaysBetweenNotices,
    ) {
    }

    public function nextNotice(CollectionCase $case, DateTimeImmutable $today): ?string
    {
        if (! $case->isOpen() || $case->status === CollectionCase::STATUS_HARDSHIP) {
            return null;
        }

        if ($case->last_notice_at !== null) {
            $sinceLast = (int) $case->last_notice_at->toDateTimeImmutable()
                ->setTime(0, 0)
                ->diff($today->setTime(0, 0))
                ->days;

            if ($sinceLast < $this->minDaysBetweenNotices) {
                return null;
            }
        }

        $order = array_keys($this->steps);
        $lastIndex = $case->last_notice === null ? -1 : array_search($case->last_notice, $order, true);
        $lastIndex = $lastIndex === false ? -1 : $lastIndex;

        $candidate = null;
        foreach ($order as $index => $notice) {
            if ($index > $lastIndex && $case->days_past_due >= $this->steps[$notice]) {
                $candidate = $notice;
            }
        }

        return $candidate;
    }
}
