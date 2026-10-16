<?php

namespace App\Services;

use App\Models\Invoice;
use App\Models\Meter;
use App\Models\Tariff;

class TariffCalculator
{
    /**
     * Energy charge for a period's usage across the tariff's consumption tiers.
     *
     * @param  float  $usageKwh  Consumption for the billing period.
     * @return int  Charge in cents.
     */
    public function tieredEnergyCents(Tariff $tariff, float $usageKwh): int
    {
        $tiers = $tariff->tiers;
        $remaining = $usageKwh;
        $previousCap = 0.0;
        $cents = 0.0;

        for ($i = 0; $i <= count($tiers); $i++) {
            $cap = $tiers[$i]['upto_kwh'];
            $band = $cap === null ? $remaining : min($remaining, $cap - $previousCap);
            $cents += max(0.0, $band) * $tiers[$i]['rate_cents'];
            $remaining -= max(0.0, $band);
            $previousCap = $cap ?? $previousCap;
        }

        return (int) round($cents);
    }

    /**
     * Daily standing charge for the meter's tariff.
     *
     * @param  int  $days  Number of chargeable days in the period.
     * @return int  Charge in cents.
     */
    public function standingChargeCents(Meter $meter, int $days): int
    {
        $daily = $meter->tariff->standing_charge_cents;

        if ($days <= 0) {
            return 0;
        }

        return $daily * $days;
    }

    /**
     * Late payment fee applied to the outstanding balance of an overdue invoice.
     *
     * @param  Invoice  $invoice  Overdue invoice.
     * @return int  Fee in cents.
     */
    public function lateFeeCents(Invoice $invoice): int
    {
        $rate = config('billing.late_fee_rate');
        $fee = $invoice->balanceCents() * $rate;

        if ($fee < 1) {
            return 0;
        }

        return (int) round($fee);
    }

    /**
     * VAT on a net amount, rounded half up to the cent.
     *
     * @param  int  $netCents  Net amount in cents.
     * @return int  VAT in cents.
     */
    public function vatCents(int $netCents): int
    {
        $basisPoints = (int) config('billing.vat_basis_points');

        if ($netCents <= 0) {
            return 0;
        }

        return intdiv($netCents * $basisPoints + 5000, 10000);
    }
}
