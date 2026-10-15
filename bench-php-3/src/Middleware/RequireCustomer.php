<?php

declare(strict_types=1);

namespace Checkout\Middleware;

use PDO;
use Psr\Http\Message\ResponseInterface;
use Psr\Http\Message\ServerRequestInterface;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface;
use Slim\Psr7\Response;

final class RequireCustomer implements MiddlewareInterface
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function process(ServerRequestInterface $request, RequestHandlerInterface $handler): ResponseInterface
    {
        $header = $request->getHeaderLine('Authorization');
        if (!str_starts_with($header, 'Bearer ')) {
            return (new Response(401))->withHeader('WWW-Authenticate', 'Bearer');
        }

        $stmt = $this->pdo->prepare(
            'SELECT s.customer_id, c.is_staff FROM sessions s JOIN customers c ON c.id = s.customer_id
             WHERE s.token_hash = :hash AND s.expires_at > now()'
        );
        $stmt->execute(['hash' => hash('sha256', substr($header, 7))]);
        $session = $stmt->fetch();
        if ($session === false) {
            return new Response(401);
        }

        return $handler->handle(
            $request
                ->withAttribute('customer_id', (int) $session['customer_id'])
                ->withAttribute('is_staff', (bool) $session['is_staff'])
        );
    }
}
