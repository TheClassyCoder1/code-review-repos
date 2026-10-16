<?php

declare(strict_types=1);

namespace Checkout\Middleware;

use Psr\Http\Message\ResponseInterface;
use Psr\Http\Message\ServerRequestInterface;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface;
use Slim\Psr7\Response;

final class SameSiteReturn implements MiddlewareInterface
{
    public function process(ServerRequestInterface $request, RequestHandlerInterface $handler): ResponseInterface
    {
        $target = (string) ($request->getQueryParams()['next'] ?? '/');

        $isRelative = str_starts_with($target, '/')
            && !str_starts_with($target, '//')
            && !str_contains($target, '\\')
            && parse_url($target, PHP_URL_HOST) === null;
        if (!$isRelative) {
            return new Response(400);
        }

        return $handler->handle($request);
    }
}
