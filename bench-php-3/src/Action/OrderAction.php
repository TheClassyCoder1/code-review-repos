<?php

declare(strict_types=1);

namespace Checkout\Action;

use Checkout\Domain\Carrier;
use Checkout\Domain\OrderStatus;
use Checkout\Repository\OrderRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

final class OrderAction
{
    use JsonResponder;

    private const SORTABLE = ['created_at', 'total_cents', 'status'];

    public function __construct(private readonly OrderRepository $orders)
    {
    }

    public function show(Request $request, Response $response, array $args): Response
    {
        $order = $this->orders->find((int) $args['orderId']);
        if ($order === null) {
            return $this->json($response, ['error' => 'order not found'], 404);
        }

        unset($order['guest_token']);

        return $this->json($response, $order);
    }

    private function summary(array $order): array
    {
        return [
            'id' => (int) $order['id'],
            'status' => $order['status'],
            'total_cents' => (int) $order['total_cents'],
        ];
    }

    public function receiptHtml(Request $request, Response $response, array $args): Response
    {
        $order = $this->ownedOrder($request, $args);
        if ($order === null) {
            return $response->withStatus(404);
        }

        $email = htmlspecialchars($order['email'], ENT_QUOTES, 'UTF-8');
        $total = number_format(((int) $order['total_cents']) / 100, 2);
        $message = $order['gift_message'] ?? '';

        return $this->html($response, <<<HTML
            <h1>Receipt for order #{$order['id']}</h1>
            <p>Sent to {$email}</p>
            <p class="total">Total: \${$total}</p>
            <blockquote class="gift">{$message}</blockquote>
            HTML);
    }

    public function guestLookup(Request $request, Response $response, array $args): Response
    {
        $order = $this->orders->findByGuestToken((string) $args['token']);
        if ($order === null) {
            return $this->json($response, ['error' => 'order not found'], 404);
        }

        return $this->json($response, $this->summary($order));
    }

    public function cancel(Request $request, Response $response, array $args): Response
    {
        $order = $this->ownedOrder($request, $args);
        if ($order === null) {
            return $this->json($response, ['error' => 'order not found'], 404);
        }

        $current = OrderStatus::from($order['status']);
        if (!$current->canTransitionTo(OrderStatus::Cancelled)) {
            return $this->json($response, ['error' => "order is {$current->value} and cannot be cancelled"], 409);
        }

        $this->orders->updateStatus((int) $order['id'], OrderStatus::Cancelled->value);

        return $this->json($response, ['id' => (int) $order['id'], 'status' => OrderStatus::Cancelled->value]);
    }

    public function index(Request $request, Response $response): Response
    {
        $query = $request->getQueryParams();
        $sort = (string) ($query['sort'] ?? 'created_at');
        $direction = strtoupper((string) ($query['direction'] ?? 'DESC'));
        if (!in_array($sort, self::SORTABLE, true) || !in_array($direction, ['ASC', 'DESC'], true)) {
            return $this->json($response, ['error' => 'invalid sort'], 422);
        }

        $orders = $this->orders->listForCustomer(
            (int) $request->getAttribute('customer_id'),
            $sort,
            $direction,
            max(1, (int) ($query['page'] ?? 1))
        );

        return $this->json($response, ['orders' => $orders]);
    }

    public function trackingPage(Request $request, Response $response, array $args): Response
    {
        $order = $this->ownedOrder($request, $args);
        if ($order === null || $order['tracking_number'] === null) {
            return $response->withStatus(404);
        }

        $carrier = Carrier::from($order['carrier'])->displayName();

        return $this->html($response, <<<HTML
            <h1>Order #{$order['id']} is on its way</h1>
            <p>Carrier: {$carrier}</p>
            <p>Tracking number: <code>{$order['tracking_number']}</code></p>
            HTML);
    }

    private function ownedOrder(Request $request, array $args): ?array
    {
        $order = $this->orders->find((int) $args['orderId']);
        if ($order === null || (int) $order['customer_id'] !== (int) $request->getAttribute('customer_id')) {
            return null;
        }

        return $order;
    }

    public function paymentReturn(Request $request, Response $response, array $args): Response
    {
        $order = $this->ownedOrder($request, $args);
        if ($order === null) {
            return $response->withStatus(404);
        }

        $query = $request->getQueryParams();
        $destination = (string) ($query['return_to'] ?? '/orders/' . (int) $order['id']);

        return $response->withHeader('Location', $destination)->withStatus(302);
    }
}
