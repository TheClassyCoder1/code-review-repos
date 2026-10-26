<?php

namespace App\Part2\Sso;

/**
 * Reduces a user-supplied "next" value to a same-origin path. Anything that
 * could be read by a browser as another origin — scheme-relative "//host",
 * backslashes, absolute URLs, control characters — collapses to the
 * dashboard.
 */
final class ReturnPathNormalizer
{
    public const FALLBACK = '/dashboard';

    public function normalize(?string $next): string
    {
        if ($next === null || $next === '' || strlen($next) > 512) {
            return self::FALLBACK;
        }

        $decoded = rawurldecode($next);

        if (preg_match('/[\x00-\x1f\x7f\\\\]/', $decoded) === 1) {
            return self::FALLBACK;
        }

        if (! str_starts_with($decoded, '/') || str_starts_with($decoded, '//')) {
            return self::FALLBACK;
        }

        $parts = parse_url($next);

        if ($parts === false || isset($parts['scheme']) || isset($parts['host'])) {
            return self::FALLBACK;
        }

        $path = $parts['path'] ?? '/';

        return isset($parts['query']) ? $path . '?' . $parts['query'] : $path;
    }
}
