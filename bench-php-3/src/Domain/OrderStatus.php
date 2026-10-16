<?php

declare(strict_types=1);

namespace Checkout\Domain;

enum OrderStatus: string
{
    case Pending = 'pending';
    case Paid = 'paid';
    case Shipped = 'shipped';
    case Delivered = 'delivered';
    case Cancelled = 'cancelled';
    case Refunded = 'refunded';

    public function canTransitionTo(self $next): bool
    {
        $allowed = match ($this) {
            self::Pending => [self::Paid, self::Cancelled],
            self::Paid => [self::Shipped, self::Cancelled, self::Refunded],
            self::Shipped => [self::Delivered, self::Refunded],
            self::Delivered => [self::Refunded],
            self::Cancelled, self::Refunded => [],
        };

        return in_array($next, $allowed, true);
    }

    public function isFinal(): bool
    {
        return $this === self::Cancelled || $this === self::Refunded;
    }
}
