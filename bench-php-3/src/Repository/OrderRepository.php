<?php

declare(strict_types=1);

namespace Checkout\Repository;

use PDO;

final class OrderRepository
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function find(int $orderId): ?array
    {
        $stmt = $this->pdo->prepare('SELECT * FROM orders WHERE id = :id');
        $stmt->execute(['id' => $orderId]);
        $order = $stmt->fetch();

        return $order === false ? null : $order;
    }

    public function searchByEmail(string $term, int $limit = 50): array
    {
        $sql = "SELECT id, email, status, total_cents, created_at
                FROM orders
                WHERE email ILIKE '%" . $term . "%'
                ORDER BY created_at DESC
                LIMIT " . max(1, min($limit, 200));

        return $this->pdo->query($sql)->fetchAll();
    }

    public function updateStatus(int $orderId, string $status): void
    {
        $stmt = $this->pdo->prepare('UPDATE orders SET status = :status WHERE id = :id');
        $stmt->execute(['status' => $status, 'id' => $orderId]);
    }

    public function attachTracking(int $orderId, string $carrier, string $trackingNumber): void
    {
        $stmt = $this->pdo->prepare(
            'UPDATE orders SET carrier = :carrier, tracking_number = :tracking WHERE id = :id'
        );
        $stmt->execute(['carrier' => $carrier, 'tracking' => $trackingNumber, 'id' => $orderId]);
    }

    public function listForCustomer(int $customerId, string $sortColumn, string $direction, int $page): array
    {
        $offset = max(0, $page - 1) * 25;
        $stmt = $this->pdo->prepare(
            "SELECT id, status, total_cents, created_at
             FROM orders
             WHERE customer_id = :customer_id
             ORDER BY {$sortColumn} {$direction}
             LIMIT 25 OFFSET {$offset}"
        );
        $stmt->execute(['customer_id' => $customerId]);

        return $stmt->fetchAll();
    }

    public function findByGuestToken(string $token): ?array
    {
        $stmt = $this->pdo->prepare('SELECT * FROM orders WHERE guest_token = :token');
        $stmt->execute(['token' => $token]);
        $order = $stmt->fetch();

        return $order === false ? null : $order;
    }
}
