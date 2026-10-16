<?php

declare(strict_types=1);

namespace Checkout\Repository;

use PDO;

final class CartRepository
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function create(int $customerId, string $reference): int
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO carts (customer_id, reference) VALUES (:customer_id, :reference) RETURNING id'
        );
        $stmt->execute(['customer_id' => $customerId, 'reference' => $reference]);

        return (int) $stmt->fetchColumn();
    }

    public function addItem(int $cartId, string $sku, int $quantity, int $unitPriceCents): void
    {
        if ($quantity < 1 || $quantity > 99) {
            throw new \InvalidArgumentException('Quantity must be between 1 and 99');
        }

        $stmt = $this->pdo->prepare(
            'INSERT INTO cart_items (cart_id, sku, quantity, unit_price_cents)
             VALUES (:cart_id, :sku, :quantity, :price)
             ON CONFLICT (cart_id, sku)
             DO UPDATE SET quantity = LEAST(cart_items.quantity + EXCLUDED.quantity, 99)'
        );
        $stmt->execute([
            'cart_id' => $cartId,
            'sku' => $sku,
            'quantity' => $quantity,
            'price' => $unitPriceCents,
        ]);
    }

    public function removeItem(int $cartId, string $sku): bool
    {
        $stmt = $this->pdo->prepare('DELETE FROM cart_items WHERE cart_id = :cart_id AND sku = :sku');
        $stmt->execute(['cart_id' => $cartId, 'sku' => $sku]);

        return $stmt->rowCount() > 0;
    }

    /**
     * @return list<array{sku: string, quantity: int, unit_price_cents: int}>
     */
    public function itemsForCart(int $cartId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT sku, quantity, unit_price_cents FROM cart_items WHERE cart_id = :cart_id ORDER BY sku'
        );
        $stmt->execute(['cart_id' => $cartId]);

        return array_map(
            static fn (array $row): array => [
                'sku' => (string) $row['sku'],
                'quantity' => (int) $row['quantity'],
                'unit_price_cents' => (int) $row['unit_price_cents'],
            ],
            $stmt->fetchAll()
        );
    }

    public function findByReference(int $customerId, string $reference): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT id, reference, coupon_code FROM carts WHERE reference = :reference AND customer_id = :customer_id'
        );
        $stmt->execute(['reference' => $reference, 'customer_id' => $customerId]);
        $cart = $stmt->fetch();

        return $cart === false ? null : $cart;
    }

    /**
     * @param list<array{sku: string, quantity: int, unit_price_cents: int}> $items
     */
    public function subtotalCents(array $items): int
    {
        $total = 0;
        foreach ($items as $item) {
            if ($item['quantity'] <= 0 || $item['unit_price_cents'] < 0) {
                throw new \DomainException('Invalid cart line for ' . $item['sku']);
            }
            $total += $item['quantity'] * $item['unit_price_cents'];
        }

        return $total;
    }

    public function replaceItems(int $cartId, array $items): void
    {
        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare('DELETE FROM cart_items WHERE cart_id = :cart_id')->execute(['cart_id' => $cartId]);
            foreach ($items as $item) {
                $this->addItem($cartId, (string) $item['sku'], (int) $item['quantity'], (int) $item['unit_price_cents']);
            }
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }
    }
}
