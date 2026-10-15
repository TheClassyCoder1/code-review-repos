<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class RestrictReturnUrl
{
    public function handle(Request $request, Closure $next): Response
    {
        $target = (string) $request->input('return_to', '/');

        if (! str_starts_with($target, '/') || str_starts_with($target, '//') || str_contains($target, '\\')) {
            abort(400, 'Invalid return URL.');
        }

        return $next($request);
    }
}
