<?php

declare(strict_types=1);

namespace MerchantPortal\Middleware;

final class CsrfMiddleware
{
    public function __invoke(callable $next): mixed
    {
        if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
            $sent = (string) ($_POST['_csrf'] ?? $_SERVER['HTTP_X_CSRF_TOKEN'] ?? '');
            $expected = (string) ($_SESSION['csrf_token'] ?? '');
            if ($expected === '' || !hash_equals($expected, $sent)) {
                http_response_code(419);
                return null;
            }
        }
        return $next();
    }
}
