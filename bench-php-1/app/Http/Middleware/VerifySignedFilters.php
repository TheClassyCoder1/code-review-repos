<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class VerifySignedFilters
{
    public function handle(Request $request, Closure $next): Response
    {
        $view = (string) $request->query('view', '');
        $signature = (string) $request->query('sig', '');

        $expected = hash_hmac('sha256', $view, config('app.key'));

        if ($view === '' || ! hash_equals($expected, $signature)) {
            abort(403, 'Saved view signature mismatch.');
        }

        return $next($request);
    }
}
