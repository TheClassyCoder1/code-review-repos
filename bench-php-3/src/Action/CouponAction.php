<?php

declare(strict_types=1);

namespace Checkout\Action;

use Checkout\Domain\OrderStatus;
use Checkout\Repository\CartRepository;
use Checkout\Repository\CouponRepository;
use DateTimeImmutable;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

final class CouponAction
{
    use JsonResponder;

    public function __construct(
        private readonly CouponRepository $coupons,
        private readonly CartRepository $carts,
    ) {
    }

    public function apply(Request $request, Response $response, array $args): Response
    {
        $body = (array) $request->getParsedBody();
        $coupon = $this->coupons->findActiveByCode((string) ($body['code'] ?? ''), new DateTimeImmutable());
        if ($coupon === null) {
            return $this->json($response, ['error' => 'coupon not found or expired'], 404);
        }
        if ($coupon['max_uses'] !== null && $this->coupons->countRedemptions($coupon['code']) >= (int) $coupon['max_uses']) {
            return $this->json($response, ['error' => 'coupon fully redeemed'], 409);
        }

        $subtotal = $this->carts->subtotalCents($this->carts->itemsForCart((int) $request->getAttribute('cart_id')));
        $discount = $coupon['percent_off'] !== null
            ? intdiv($subtotal * (int) $coupon['percent_off'], 100)
            : min($subtotal, (int) $coupon['amount_off']);

        return $this->json($response, [
            'code' => $coupon['code'],
            'subtotal_cents' => $subtotal,
            'discount_cents' => $discount,
            'total_cents' => $subtotal - $discount,
        ]);
    }

    public function report(Request $request, Response $response): Response
    {
        $status = OrderStatus::tryFrom((string) ($request->getQueryParams()['status'] ?? 'paid'));
        if ($status === null) {
            return $this->json($response, ['error' => 'unknown order status'], 422);
        }

        return $this->json($response, [
            'status' => $status->value,
            'coupons' => $this->coupons->redemptionsByStatus($status->value),
        ]);
    }
}
