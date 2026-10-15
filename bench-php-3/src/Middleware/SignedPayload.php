<?php

declare(strict_types=1);

namespace Checkout\Middleware;

use Psr\Http\Message\ResponseInterface;
use Psr\Http\Message\ServerRequestInterface;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface;
use Slim\Psr7\Response;

final class SignedPayload implements MiddlewareInterface
{
    public function __construct(private readonly string $signingKey)
    {
    }

    public function process(ServerRequestInterface $request, RequestHandlerInterface $handler): ResponseInterface
    {
        $body = (array) $request->getParsedBody();
        $payload = (string) ($body['snapshot'] ?? '');
        $signature = (string) ($body['signature'] ?? '');

        $expected = hash_hmac('sha256', $payload, $this->signingKey);
        if ($this->signingKey === '' || $payload === '' || !hash_equals($expected, $signature)) {
            return new Response(400);
        }

        return $handler->handle($request);
    }
}
