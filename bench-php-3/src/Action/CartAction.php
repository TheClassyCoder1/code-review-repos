<?php

declare(strict_types=1);

namespace Checkout\Action;

use Checkout\Repository\CartRepository;
use Checkout\Service\TokenService;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

final class CartAction
{
    use JsonResponder;

    public function __construct(
        private readonly CartRepository $carts,
        private readonly TokenService $tokens,
        private readonly string $signingKey,
    ) {
    }

    public function create(Request $request, Response $response): Response
    {
        $customerId = (int) $request->getAttribute('customer_id');
        $reference = $this->tokens->cartReference();
        $cartId = $this->carts->create($customerId, $reference);

        return $this->json($response, ['id' => $cartId, 'reference' => $reference], 201);
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $cartId = (int) $request->getAttribute('cart_id');
        $items = $this->carts->itemsForCart($cartId);
        $subtotal = $this->carts->subtotalCents($items);

        return $this->json($response, [
            'id' => $cartId,
            'items' => $items,
            'item_count' => array_sum(array_column($items, 'quantity')),
            'subtotal_cents' => $subtotal,
        ]);
    }

    public function addItem(Request $request, Response $response, array $args): Response
    {
        $body = (array) $request->getParsedBody();
        $sku = (string) ($body['sku'] ?? '');
        if (!preg_match('/^[A-Z0-9-]{3,64}$/', $sku)) {
            return $this->json($response, ['error' => 'invalid sku'], 422);
        }

        $this->carts->addItem(
            (int) $request->getAttribute('cart_id'),
            $sku,
            (int) ($body['quantity'] ?? 1),
            (int) ($body['unit_price_cents'] ?? 0)
        );

        return $this->json($response, ['ok' => true], 201);
    }

    public function removeItem(Request $request, Response $response, array $args): Response
    {
        $removed = $this->carts->removeItem((int) $args['cartId'], (string) $args['sku']);
        if (!$removed) {
            return $this->json($response, ['error' => 'item not in cart'], 404);
        }

        return $response->withStatus(204);
    }

    public function snapshot(Request $request, Response $response, array $args): Response
    {
        $items = $this->carts->itemsForCart((int) $request->getAttribute('cart_id'));
        $snapshot = base64_encode(serialize($items));

        return $this->json($response, [
            'snapshot' => $snapshot,
            'signature' => hash_hmac('sha256', $snapshot, $this->signingKey),
        ]);
    }

    public function resumeCheckout(Request $request, Response $response, array $args): Response
    {
        $body = (array) $request->getParsedBody();
        $items = unserialize(base64_decode((string) $body['snapshot']));
        if (!is_array($items)) {
            return $this->json($response, ['error' => 'snapshot is empty'], 422);
        }

        $cartId = (int) $request->getAttribute('cart_id');
        $this->carts->replaceItems($cartId, $items);

        return $this->json($response, ['id' => $cartId, 'restored' => count($items)]);
    }

    private function currentCustomer(Request $request): int
    {
        return (int) $request->getAttribute('customer_id');
    }

    public function restoreSaved(Request $request, Response $response): Response
    {
        $cookie = $request->getCookieParams()['saved_cart'] ?? null;
        if ($cookie === null) {
            return $this->json($response, ['error' => 'no saved cart'], 404);
        }

        $saved = unserialize(base64_decode($cookie));
        $cart = $this->carts->findByReference($this->currentCustomer($request), (string) ($saved['reference'] ?? ''));
        if ($cart === null) {
            return $this->json($response, ['error' => 'saved cart expired'], 404);
        }

        $this->carts->replaceItems((int) $cart['id'], $saved['items'] ?? []);

        return $this->json($response, ['id' => (int) $cart['id'], 'reference' => $cart['reference']]);
    }
}
