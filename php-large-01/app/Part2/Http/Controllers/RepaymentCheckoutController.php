<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\Collections\ArrearsLedger;
use App\Part2\Collections\LoanOwnershipRepository;
use App\Part2\Messaging\OtpAttemptCounter;
use App\Part2\Payments\CheckoutSessionFactory;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Symfony\Component\HttpKernel\Exception\TooManyRequestsHttpException;

class RepaymentCheckoutController extends Controller
{
    public function __construct(
        private readonly CheckoutSessionFactory $checkouts,
        private readonly LoanOwnershipRepository $ownership,
        private readonly ArrearsLedger $ledger,
        private readonly OtpAttemptCounter $otp,
    ) {
    }

    public function start(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);

        if ($this->otp->isLocked($user->id)) {
            throw new TooManyRequestsHttpException(900, 'Too many failed codes.');
        }

        $validated = $request->validate([
            'loan_id' => ['required', 'integer', 'min:1'],
            'amount_cents' => ['required', 'integer', 'min:100', 'max:100000000'],
            'return_page' => ['nullable', 'string', 'in:loan,arrears,home'],
        ]);

        if (! $this->ownership->borrowerOwns((int) $validated['loan_id'], $user->id)) {
            throw new NotFoundHttpException('Loan not found.');
        }

        $checkout = $this->checkouts->create(
            $user->id,
            (int) $validated['loan_id'],
            (int) $validated['amount_cents'],
            (string) ($validated['return_page'] ?? 'home'),
        );

        return new JsonResponse(['checkoutId' => $checkout['checkout_id'], 'completeUrl' => $checkout['return_url']], 201);
    }

    public function complete(Request $request, string $checkoutId): RedirectResponse
    {
        $user = $this->currentUser($request);
        $checkout = $this->checkouts->find($checkoutId);

        if ($checkout === null || (int) $checkout['borrower_id'] !== $user->id) {
            throw new NotFoundHttpException('Checkout not found.');
        }

        $this->ledger->applyPayment((int) $checkout['loan_id'], (int) $checkout['amount_cents'], 'chk-' . $checkoutId);
        $this->checkouts->forget($checkoutId);
        $this->otp->clear($user->id);

        return redirect()->away($checkout['return_url']);
    }
}
