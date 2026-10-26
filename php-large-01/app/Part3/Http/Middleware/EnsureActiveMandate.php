<?php

namespace App\Part3\Http\Middleware;

use App\Part3\Repositories\MandateRepository;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

/**
 * Resolves the {mandate} route parameter to an active mandate owned by the
 * caller. Anything else is a 404, so controllers and services behind this
 * middleware can rely on the mandate existing and being active.
 */
class EnsureActiveMandate
{
    public function __construct(private readonly MandateRepository $mandates)
    {
    }

    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        $mandateId = (int) $request->route('mandate');

        if ($user === null || $this->mandates->findActiveForBorrower($mandateId, (int) $user->getAuthIdentifier()) === null) {
            throw new NotFoundHttpException('Mandate not found.');
        }

        return $next($request);
    }
}
