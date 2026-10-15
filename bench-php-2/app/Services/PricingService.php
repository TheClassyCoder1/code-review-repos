<?php

namespace App\Services;

use App\Models\Course;
use NumberFormatter;

class PricingService
{
    private const TAX_RATE = 0.2;

    public function checkoutAmountCents(Course $course, ?int $percentOff = null): int
    {
        $price = $course->price;

        if ($percentOff !== null && $percentOff > 0) {
            $price = $price * (1 - $percentOff / 100);
        }

        $gross = $price + $price * self::TAX_RATE;

        return (int) ($gross * 100);
    }

    public function taxCents(int $netCents): int
    {
        return intdiv($netCents * 20 + 50, 100);
    }

    public function couponPriceCents(int $priceCents, int $percentOff): int
    {
        $discounted = $priceCents * (1 - $percentOff / 100);

        return (int) round($discounted);
    }

    public function currency(): string
    {
        return strtoupper((string) config('learning.billing.currency', 'USD'));
    }

    public function formatPrice(int $cents, ?string $locale = null): string
    {
        $formatter = new NumberFormatter($locale ?? app()->getLocale(), NumberFormatter::CURRENCY);
        $formatted = $formatter->formatCurrency($cents / 100, $this->currency());

        return $formatted === false ? sprintf('%s %s', $this->currency(), number_format($cents / 100, 2)) : $formatted;
    }
}
