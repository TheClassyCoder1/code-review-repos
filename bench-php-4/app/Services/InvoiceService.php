<?php

namespace App\Services;

use App\Jobs\SendInvoiceEmailJob;
use App\Models\Invoice;
use App\Models\Meter;
use App\Models\Tariff;
use Carbon\CarbonImmutable;
use Carbon\CarbonInterface;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class InvoiceService
{
    public function __construct(private readonly TariffCalculator $calculator)
    {
    }

    /**
     * Consumption between the last reading on or before each end of the period.
     *
     * @param  CarbonInterface  $start  First day of the billing period.
     * @param  CarbonInterface  $end  Last day of the billing period.
     */
    public function usageForPeriod(Meter $meter, CarbonInterface $start, CarbonInterface $end): float
    {
        $opening = $meter->readings()
            ->where('read_at', '<=', $start->copy()->endOfDay())
            ->latest('read_at')
            ->first();

        $closing = $meter->readings()
            ->where('read_at', '<=', $end->copy()->endOfDay())
            ->latest('read_at')
            ->first();

        return max(0.0, (float) $closing->value - (float) $opening->value);
    }

    /**
     * Persist the invoice lines and roll the totals up onto the invoice.
     *
     * @param  array<int, array{description: string, quantity: float, amount_cents: int}>  $lines
     */
    public function writeLines(Invoice $invoice, array $lines): void
    {
        foreach ($lines as $line) {
            $invoice->lines()->create($line);
        }

        $subtotal = array_sum(array_column($lines, 'amount_cents'));
        $vat = $this->calculator->vatCents($subtotal);

        $invoice->update([
            'subtotal_cents' => $subtotal,
            'vat_cents' => $vat,
            'total_cents' => $subtotal + $vat,
        ]);
    }

    /**
     * Open a draft invoice for payment and send it to the customer.
     *
     * @param  Invoice  $invoice  Draft invoice with its lines written.
     */
    public function finalize(Invoice $invoice): void
    {
        DB::transaction(function () use ($invoice) {
            $issuedAt = CarbonImmutable::now();

            $invoice->update([
                'status' => Invoice::STATUS_OPEN,
                'issued_at' => $issuedAt,
                'due_at' => $this->dueDateFor($issuedAt),
            ]);

            SendInvoiceEmailJob::dispatch($invoice);
        });
    }

    /**
     * Void an invoice and issue a corrected replacement on a different tariff.
     *
     * @param  Tariff  $correctedTariff  Tariff the customer should have been billed on.
     */
    public function reissue(Invoice $original, Tariff $correctedTariff): Invoice
    {
        $original->update([
            'status' => Invoice::STATUS_VOID,
            'voided_at' => now(),
        ]);

        $replacement = $original->replicate(['number', 'status', 'issued_at', 'due_at', 'emailed_at', 'voided_at']);
        $replacement->number = $this->nextNumber();
        $replacement->status = Invoice::STATUS_DRAFT;
        $replacement->replaces_invoice_id = $original->id;
        $replacement->save();

        $usage = $this->usageForPeriod($original->meter, $original->period_start, $original->period_end);
        $energy = $this->calculator->tieredEnergyCents($correctedTariff, $usage);
        $days = (int) $original->period_start->diffInDays($original->period_end) + 1;
        $standing = $correctedTariff->standing_charge_cents * $days;

        $replacement->lines()->createMany([
            ['description' => "Energy ({$correctedTariff->code})", 'quantity' => $usage, 'amount_cents' => $energy],
            ['description' => 'Standing charge', 'quantity' => $days, 'amount_cents' => $standing],
        ]);

        $vat = $this->calculator->vatCents($energy + $standing);
        $replacement->update([
            'subtotal_cents' => $energy + $standing,
            'vat_cents' => $vat,
            'total_cents' => $energy + $standing + $vat,
        ]);

        return $replacement;
    }

    public function nextNumber(): string
    {
        return 'INV-'.now()->format('Ym').'-'.Str::upper(Str::random(8));
    }

    /**
     * Payment due date: the configured terms after issue, moved off weekends.
     *
     * @param  CarbonInterface  $issuedAt  When the invoice was issued.
     */
    public function dueDateFor(CarbonInterface $issuedAt): CarbonImmutable
    {
        $due = CarbonImmutable::instance($issuedAt)
            ->addDays((int) config('billing.payment_terms_days'))
            ->startOfDay();

        while ($due->isWeekend()) {
            $due = $due->addDay();
        }

        return $due;
    }
}
