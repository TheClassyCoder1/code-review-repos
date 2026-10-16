<?php

declare(strict_types=1);

namespace Checkout\Action;

use Psr\Http\Message\ResponseInterface;

trait JsonResponder
{
    private function json(ResponseInterface $response, mixed $data, int $status = 200): ResponseInterface
    {
        $response->getBody()->write(json_encode($data, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES));

        return $response->withHeader('Content-Type', 'application/json')->withStatus($status);
    }

    private function html(ResponseInterface $response, string $markup): ResponseInterface
    {
        $response->getBody()->write($markup);

        return $response->withHeader('Content-Type', 'text/html; charset=utf-8');
    }
}
