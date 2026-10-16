<?php

declare(strict_types=1);

use Checkout\Action\AccountAction;
use Checkout\Action\CartAction;
use Checkout\Action\CouponAction;
use Checkout\Action\OrderAction;
use Checkout\Action\ShippingLabelAction;
use Checkout\Middleware\CartOwnership;
use Checkout\Middleware\RequireCustomer;
use Checkout\Middleware\RequireStaff;
use Checkout\Middleware\SameSiteReturn;
use Checkout\Middleware\SignedPayload;
use Slim\App;
use Slim\Routing\RouteCollectorProxy;

return static function (App $app): void {
    $app->post('/accounts', [AccountAction::class, 'register']);
    $app->post('/sessions', [AccountAction::class, 'login']);
    $app->get('/orders/guest/{token}', [OrderAction::class, 'guestLookup']);

    $app->group('', function (RouteCollectorProxy $customer): void {
        $customer->patch('/accounts/me', [AccountAction::class, 'updateProfile']);
        $customer->get('/logout', [AccountAction::class, 'logout'])->add(SameSiteReturn::class);

        $customer->post('/carts', [CartAction::class, 'create']);
        $customer->post('/carts/restore', [CartAction::class, 'restoreSaved']);
        $customer->group('/carts/{cartId:[0-9]+}', function (RouteCollectorProxy $cart): void {
            $cart->get('', [CartAction::class, 'show']);
            $cart->post('/items', [CartAction::class, 'addItem']);
            $cart->delete('/items/{sku}', [CartAction::class, 'removeItem']);
            $cart->get('/snapshot', [CartAction::class, 'snapshot']);
            $cart->post('/resume', [CartAction::class, 'resumeCheckout'])->add(SignedPayload::class);
            $cart->post('/coupon', [CouponAction::class, 'apply']);
        })->add(CartOwnership::class);

        $customer->get('/orders', [OrderAction::class, 'index']);
        $customer->get('/orders/{orderId:[0-9]+}', [OrderAction::class, 'show']);
        $customer->get('/orders/{orderId:[0-9]+}/receipt', [OrderAction::class, 'receiptHtml']);
        $customer->post('/orders/{orderId:[0-9]+}/cancel', [OrderAction::class, 'cancel']);
        $customer->get('/orders/{orderId:[0-9]+}/tracking', [OrderAction::class, 'trackingPage']);
        $customer->get('/orders/{orderId:[0-9]+}/payment-return', [OrderAction::class, 'paymentReturn']);
        $customer->post('/orders/{orderId:[0-9]+}/shipping-quote', [ShippingLabelAction::class, 'quote']);
        $customer->get('/orders/{orderId:[0-9]+}/labels', [ShippingLabelAction::class, 'download']);
        $customer->post('/orders/{orderId:[0-9]+}/return-label', [ShippingLabelAction::class, 'attachReturnLabel']);

        $customer->group('/fulfillment', function (RouteCollectorProxy $staff): void {
            $staff->post('/orders/{orderId:[0-9]+}/labels', [ShippingLabelAction::class, 'create']);
            $staff->post('/labels/{labelId:[0-9]+}/print', [ShippingLabelAction::class, 'printLabel']);
            $staff->post('/labels/{labelId:[0-9]+}/void', [ShippingLabelAction::class, 'void']);
            $staff->get('/reports/coupons', [CouponAction::class, 'report']);
        })->add(RequireStaff::class);
    })->add(RequireCustomer::class);
};
