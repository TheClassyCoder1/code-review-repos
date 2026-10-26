<?php

namespace App\Part5\Collections;

use DateTimeImmutable;
use DateTimeZone;
use Exception;

/**
 * Collections contact is only allowed inside the borrower's local calling
 * hours. Evaluated in the borrower's timezone, falling back to the
 * platform default when the stored zone is missing or invalid.
 */
class ContactWindowPolicy
{
    public function __construct(
        private readonly int $startHour,
        private readonly int $endHour,
        private readonly bool $sundayAllowed,
        private readonly string $defaultTimezone = 'Europe/Berlin',
    ) {
    }

    public function canContactAt(DateTimeImmutable $instant, ?string $borrowerTimezone): bool
    {
        $local = $instant->setTimezone($this->zoneFor($borrowerTimezone));

        if (! $this->sundayAllowed && $local->format('N') === '7') {
            return false;
        }

        $minutes = ((int) $local->format('G')) * 60 + (int) $local->format('i');

        return $minutes >= $this->startHour * 60 && $minutes < $this->endHour * 60;
    }

    /**
     * Next instant (on a minute boundary) at which contact is allowed,
     * scanning at most a week ahead.
     */
    public function nextAllowedAt(DateTimeImmutable $instant, ?string $borrowerTimezone): ?DateTimeImmutable
    {
        if ($this->canContactAt($instant, $borrowerTimezone)) {
            return $instant;
        }

        $zone = $this->zoneFor($borrowerTimezone);
        $local = $instant->setTimezone($zone);

        for ($day = 0; $day <= 7; $day++) {
            $candidate = $local->modify("+{$day} days")->setTime($this->startHour, 0);
            if ($candidate > $local && $this->canContactAt($candidate, $borrowerTimezone)) {
                return $candidate->setTimezone($instant->getTimezone());
            }
        }

        return null;
    }

    private function zoneFor(?string $timezone): DateTimeZone
    {
        if ($timezone !== null && $timezone !== '') {
            try {
                return new DateTimeZone($timezone);
            } catch (Exception) {
                // fall through to the default zone
            }
        }

        return new DateTimeZone($this->defaultTimezone);
    }
}
