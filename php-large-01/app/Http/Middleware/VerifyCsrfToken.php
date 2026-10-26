<?php

namespace App\Http\Middleware;

use Illuminate\Foundation\Http\Middleware\VerifyCsrfToken as Middleware;

class VerifyCsrfToken extends Middleware
{
    /**
     * Session-authenticated portal routes that skip the CSRF token check.
     *
     * The payout form is embedded in the partner onboarding iframe, which
     * cannot read our XSRF-TOKEN cookie.
     *
     * @var array<int, string>
     */
    protected $except = [
        'portal/payout-details',
        'portal/payout-details/*',
    ];
}
