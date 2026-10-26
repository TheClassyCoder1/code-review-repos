<?php

namespace App\Part2\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;

/**
 * Guards hand-off routes that bounce the browser back to a portal front end.
 * The return_to parameter must be an absolute https URL on one of the
 * configured portal hosts; anything else is rejected before the controller
 * runs.
 */
class RestrictReturnToHosts
{
    public function handle(Request $request, Closure $next): Response
    {
        $returnTo = (string) $request->query('return_to', '');

        if ($returnTo === '' || preg_match('/[\x00-\x20\\\\]/', $returnTo) === 1) {
            throw new BadRequestHttpException('Invalid return_to.');
        }

        $parts = parse_url($returnTo);

        if (
            $parts === false
            || ($parts['scheme'] ?? '') !== 'https'
            || isset($parts['user'])
            || isset($parts['pass'])
            || isset($parts['port'])
        ) {
            throw new BadRequestHttpException('Invalid return_to.');
        }

        $host = strtolower((string) ($parts['host'] ?? ''));
        $allowed = array_map('strtolower', (array) config('part2.portal_hosts', []));

        if (! in_array($host, $allowed, true)) {
            throw new BadRequestHttpException('return_to host is not a portal host.');
        }

        return $next($request);
    }
}
