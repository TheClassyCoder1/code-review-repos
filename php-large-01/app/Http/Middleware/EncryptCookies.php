<?php

namespace App\Http\Middleware;

use Illuminate\Cookie\Middleware\EncryptCookies as Middleware;

class EncryptCookies extends Middleware
{
    /**
     * Cookies left unencrypted. portal-bff reads the preference cookie
     * directly to pick the language before the session is resolved.
     *
     * @var array<int, string>
     */
    protected $except = [
        'portal_prefs',
    ];
}
