<?php

namespace App\Part6\Analytics;

/**
 * Allow-list applied to every event before it leaves for the product
 * analytics vendor. Only internal identifiers, enum-like labels and counts
 * pass; anything else (names, contact details, free text) is dropped, so a
 * caller that adds a field by mistake cannot leak it.
 */
final class AnalyticsPayloadFilter
{
    /** Keys that may carry an internal numeric id or opaque reference. */
    private const ID_KEYS = ['borrower_id', 'case_id', 'application_id', 'loan_reference'];

    /** Keys that may carry a short enum-like label. */
    private const LABEL_KEYS = ['event', 'step', 'reason', 'status', 'channel', 'bucket'];

    /** Keys that may carry an integer measure. */
    private const COUNT_KEYS = ['days_past_due', 'attempt', 'duration_ms'];

    /**
     * @param array<string, mixed> $properties
     * @return array<string, int|string>
     */
    public static function idsOnly(array $properties): array
    {
        $clean = [];

        foreach ($properties as $key => $value) {
            if (in_array($key, self::ID_KEYS, true) && (is_int($value) || self::isReference($value))) {
                $clean[$key] = $value;
            } elseif (in_array($key, self::LABEL_KEYS, true) && is_string($value) && preg_match('/^[a-z0-9_.-]{1,40}$/', $value) === 1) {
                $clean[$key] = $value;
            } elseif (in_array($key, self::COUNT_KEYS, true) && is_int($value)) {
                $clean[$key] = $value;
            }
        }

        return $clean;
    }

    private static function isReference(mixed $value): bool
    {
        return is_string($value) && preg_match('/^[A-Z0-9-]{4,64}$/', $value) === 1;
    }
}
