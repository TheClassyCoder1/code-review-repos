<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class VerifyGatewaySignature
{
    /**
     * Reject gateway webhooks whose HMAC signature does not match the raw body.
     *
     * @param  Closure(Request): Response  $next
     * @return Response
     */
    public function handle(Request $request, Closure $next): Response
    {
        $secret = (string) config('billing.gateway.webhook_secret');
        $signature = (string) $request->header('X-Paygate-Signature');

        if ($secret === '' || $signature === '') {
            abort(401);
        }

        $expected = hash_hmac('sha256', $request->getContent(), $secret);

        if (! hash_equals($expected, $signature)) {
            abort(401);
        }

        return $next($request);
    }
}
