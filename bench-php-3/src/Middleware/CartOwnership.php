<?php

declare(strict_types=1);

namespace Checkout\Middleware;

use PDO;
use Psr\Http\Message\ResponseInterface;
use Psr\Http\Message\ServerRequestInterface;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface;
use Slim\Psr7\Response;
use Slim\Routing\RouteContext;

final class CartOwnership implements MiddlewareInterface
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function process(ServerRequestInterface $request, RequestHandlerInterface $handler): ResponseInterface
    {
        $route = RouteContext::fromRequest($request)->getRoute();
        $cartId = (int) ($route?->getArgument('cartId') ?? 0);

        $stmt = $this->pdo->prepare('SELECT customer_id FROM carts WHERE id = :id');
        $stmt->execute(['id' => $cartId]);
        $ownerId = $stmt->fetchColumn();

        if ($ownerId === false) {
            return new Response(404);
        }
        if ((int) $ownerId !== $request->getAttribute('customer_id')) {
            return new Response(403);
        }

        return $handler->handle($request->withAttribute('cart_id', $cartId));
    }
}
