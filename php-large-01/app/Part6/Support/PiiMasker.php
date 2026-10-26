<?php

namespace App\Part6\Support;

/**
 * Masks borrower contact data before it reaches a log line or an audit row.
 * The output keeps just enough to tell two values apart during support
 * triage ("j***@e***.com", "*******4821") and never the full value.
 */
final class PiiMasker
{
    public static function email(string $email): string
    {
        $email = trim($email);
        $at = strrpos($email, '@');

        if ($at === false || $at === 0) {
            return str_repeat('*', max(3, strlen($email)));
        }

        $local = substr($email, 0, $at);
        $domain = substr($email, $at + 1);
        $dot = strrpos($domain, '.');
        $host = $dot === false ? $domain : substr($domain, 0, $dot);
        $tld = $dot === false ? '' : substr($domain, $dot);

        return self::keepFirst($local) . '@' . self::keepFirst($host) . $tld;
    }

    public static function phone(string $phone): string
    {
        $digits = preg_replace('/\D+/', '', $phone) ?? '';
        $length = strlen($digits);

        if ($length <= 4) {
            return str_repeat('*', $length);
        }

        return str_repeat('*', $length - 4) . substr($digits, -4);
    }

    private static function keepFirst(string $value): string
    {
        if ($value === '') {
            return '***';
        }

        return substr($value, 0, 1) . '***';
    }
}
