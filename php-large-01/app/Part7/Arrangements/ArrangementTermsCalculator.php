<?php

namespace App\Part7\Arrangements;

use App\Part4\Support\Money;
use DateTimeImmutable;
use InvalidArgumentException;

/**
 * Turns an arrears balance into a monthly instalment schedule.
 *
 * The balance is split with Money::allocate so the instalments always sum
 * back to the arrears exactly; any rounding cent lands on the earliest
 * instalments. Due dates keep the borrower's chosen day of month and clamp
 * to the month's last day (31 Jan -> 28/29 Feb -> 31 Mar).
 */
final class ArrangementTermsCalculator
{
    public function __construct(
        private readonly string $currency,
        private readonly int $minInstalments,
        private readonly int $maxInstalments,
        private readonly int $minInstalmentCents,
        private readonly int $maxFirstDueDays,
    ) {
        if ($minInstalments < 1 || $maxInstalments < $minInstalments) {
            throw new InvalidArgumentException('Instalment bounds are inconsistent.');
        }
    }

    /**
     * @return array{instalment_count: int, instalment_cents: int, schedule: list<array{sequence: int, due_on: string, amount_cents: int}>}
     */
    public function quote(int $arrearsCents, int $count, DateTimeImmutable $firstDue, DateTimeImmutable $today): array
    {
        $this->assertTerms($arrearsCents, $count, $firstDue, $today);

        $parts = Money::ofMinor($arrearsCents, $this->currency)->allocate(array_fill(0, $count, 1));
        $schedule = [];

        foreach ($parts as $index => $part) {
            $schedule[] = [
                'sequence' => $index + 1,
                'due_on' => self::monthsAfter($firstDue, $index)->format('Y-m-d'),
                'amount_cents' => $part->minor,
            ];
        }

        return [
            'instalment_count' => $count,
            // The headline figure shown to the borrower is the largest instalment.
            'instalment_cents' => $schedule[0]['amount_cents'],
            'schedule' => $schedule,
        ];
    }

    /**
     * Fewest instalments that keep each one at or below the given amount,
     * within the configured bounds. Returns null when even the maximum term
     * cannot bring the instalment down that far.
     */
    public function countForTarget(int $arrearsCents, int $targetInstalmentCents): ?int
    {
        if ($arrearsCents <= 0 || $targetInstalmentCents <= 0) {
            return null;
        }

        $count = max($this->minInstalments, intdiv($arrearsCents + $targetInstalmentCents - 1, $targetInstalmentCents));

        return $count <= $this->maxInstalments ? $count : null;
    }

    public static function monthsAfter(DateTimeImmutable $start, int $months): DateTimeImmutable
    {
        $day = (int) $start->format('j');
        $firstOfMonth = $start->modify('first day of this month')->modify("+{$months} months");
        $lastDay = (int) $firstOfMonth->format('t');

        return $firstOfMonth->setDate(
            (int) $firstOfMonth->format('Y'),
            (int) $firstOfMonth->format('n'),
            min($day, $lastDay),
        );
    }

    private function assertTerms(int $arrearsCents, int $count, DateTimeImmutable $firstDue, DateTimeImmutable $today): void
    {
        if ($arrearsCents <= 0) {
            throw new InvalidArgumentException('There are no arrears to arrange.');
        }

        if ($count < $this->minInstalments || $count > $this->maxInstalments) {
            throw new InvalidArgumentException(
                "Choose between {$this->minInstalments} and {$this->maxInstalments} instalments."
            );
        }

        if (intdiv($arrearsCents, $count) < $this->minInstalmentCents) {
            throw new InvalidArgumentException('Each instalment would fall below the minimum payment; choose fewer instalments.');
        }

        $todayDate = $today->setTime(0, 0);
        $firstDate = $firstDue->setTime(0, 0);

        if ($firstDate <= $todayDate) {
            throw new InvalidArgumentException('The first instalment must be due after today.');
        }

        if ($firstDate > $todayDate->modify("+{$this->maxFirstDueDays} days")) {
            throw new InvalidArgumentException("The first instalment must be due within {$this->maxFirstDueDays} days.");
        }
    }
}
