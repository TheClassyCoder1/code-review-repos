<?php

namespace App\Part2\Payments;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Str;

/**
 * Creates the server-side state for a repayment checkout. The return URL is
 * never taken from the request: it is either the completion route of this
 * service or a fixed page on the configured portal, chosen by a key.
 */
class CheckoutSessionFactory
{
    private const TTL_SECONDS = 1800;

    private const PORTAL_PAGES = [
        'loan' => '/loans',
        'arrears' => '/collections/arrears',
        'home' => '/dashboard',
    ];

    public function create(int $borrowerId, int $loanId, int $amountCents, string $returnPage): array
    {
        $checkoutId = (string) Str::uuid();

        $portalBase = rtrim((string) config('services.portal.base_url'), '/');
        $page = self::PORTAL_PAGES[$returnPage] ?? self::PORTAL_PAGES['home'];

        $state = [
            'checkout_id' => $checkoutId,
            'borrower_id' => $borrowerId,
            'loan_id' => $loanId,
            'amount_cents' => $amountCents,
            'complete_url' => route('part2.repayments.complete', ['checkoutId' => $checkoutId]),
            'return_url' => $portalBase . $page . '?checkout=' . rawurlencode($checkoutId),
        ];

        Cache::put($this->key($checkoutId), $state, self::TTL_SECONDS);

        return ['checkout_id' => $checkoutId, 'return_url' => $state['complete_url']];
    }

    public function find(string $checkoutId): ?array
    {
        $state = Cache::get($this->key($checkoutId));

        return is_array($state) ? $state : null;
    }

    public function forget(string $checkoutId): void
    {
        Cache::forget($this->key($checkoutId));
    }

    private function key(string $checkoutId): string
    {
        return 'part2:checkout:' . $checkoutId;
    }
}
