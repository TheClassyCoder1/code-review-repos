<?php

declare(strict_types=1);

namespace Checkout\Repository;

use DateTimeImmutable;
use PDO;

final class CouponRepository
{
    public function __construct(private readonly PDO $pdo)
    {
    }

    public function findActiveByCode(string $code, DateTimeImmutable $now): ?array
    {
        $normalized = strtoupper(trim($code));
        if ($normalized === '' || strlen($normalized) > 40) {
            return null;
        }

        $stmt = $this->pdo->prepare(
            'SELECT code, percent_off, amount_off, max_uses, starts_at, expires_at
             FROM coupons
             WHERE code = :code AND starts_at <= :now AND (expires_at IS NULL OR expires_at > :now)'
        );
        $stmt->execute(['code' => $normalized, 'now' => $now->format(DATE_ATOM)]);
        $coupon = $stmt->fetch();

        return $coupon === false ? null : $coupon;
    }

    public function countRedemptions(string $code): int
    {
        $stmt = $this->pdo->prepare('SELECT count(*) FROM orders WHERE coupon_code = :code');
        $stmt->execute(['code' => $code]);

        return (int) $stmt->fetchColumn();
    }

    public function redemptionsByStatus(string $status): array
    {
        $sql = "SELECT coupon_code, count(*) AS uses, sum(total_cents) AS revenue_cents
                FROM orders
                WHERE coupon_code IS NOT NULL AND status = '{$status}'
                GROUP BY coupon_code
                ORDER BY uses DESC";

        return $this->pdo->query($sql)->fetchAll();
    }
}
