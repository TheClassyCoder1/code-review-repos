<?php

namespace App\Part2\Http\Middleware;

use App\Models\User;
use App\Part2\Collections\LoanOwnershipRepository;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Symfony\Component\HttpKernel\Exception\UnauthorizedHttpException;

/**
 * Rejects any request for a {loanId} route unless the signed-in borrower
 * owns that loan. Collections desk staff may view every loan.
 *
 * Answers 404 rather than 403 so loan ids cannot be probed.
 */
class EnsureBorrowerOwnsLoan
{
    public function __construct(private readonly LoanOwnershipRepository $ownership)
    {
    }

    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (! $user instanceof User) {
            throw new UnauthorizedHttpException('Bearer', 'Authentication required.');
        }

        $loanId = filter_var($request->route('loanId'), FILTER_VALIDATE_INT);

        if ($loanId === false) {
            throw new NotFoundHttpException('Loan not found.');
        }

        if (! $user->isOps() && ! $this->ownership->borrowerOwns($loanId, $user->id)) {
            throw new NotFoundHttpException('Loan not found.');
        }

        return $next($request);
    }
}
