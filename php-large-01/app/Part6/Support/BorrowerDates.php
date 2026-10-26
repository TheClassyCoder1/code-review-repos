<?php

namespace App\Part6\Support;

use DateTimeInterface;
use DateTimeZone;

/**
 * Dates as borrowers see them in the portal, letters and SMS. Always shown
 * in the borrower's own timezone with an unambiguous month name, never as
 * 03/04/2026, which reads differently on each side of the Atlantic.
 */
final class BorrowerDates
{
    public const DEFAULT_TIMEZONE = 'Europe/Berlin';

    /** "3 Apr 2026" in the borrower's timezone. */
    public static function forDisplay(DateTimeInterface $date, ?string $timezone = null): string
    {
        $local = \DateTimeImmutable::createFromInterface($date)
            ->setTimezone(new DateTimeZone($timezone ?: self::DEFAULT_TIMEZONE));

        return $local->format('j M Y');
    }

    /** Date line used in hardship decision letters. */
    public static function forLetter(DateTimeInterface $date, ?string $timezone = null): string
    {
        $tz = new DateTimeZone($timezone !== null && $timezone !== '' ? $timezone : self::DEFAULT_TIMEZONE);

        return \DateTimeImmutable::createFromInterface($date)->setTimezone($tz)->format('j M Y');
    }

    /** ISO date for machine-readable fields (API payloads, CSV). */
    public static function iso(DateTimeInterface $date): string
    {
        return $date->format('Y-m-d');
    }
}
