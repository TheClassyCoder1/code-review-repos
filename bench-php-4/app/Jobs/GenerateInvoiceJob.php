<?php

namespace App\Jobs;

use App\Models\Invoice;
use App\Models\Meter;
use App\Services\InvoiceService;
use App\Services\TariffCalculator;
use Carbon\CarbonImmutable;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Support\Facades\DB;

class GenerateInvoiceJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable;

    public int $tries = 3;

    public function __construct(
        public int $meterId,
        public string $periodStart,
        public string $periodEnd,
    ) {
    }

    public function handle(InvoiceService $invoices, TariffCalculator $calculator): void
    {
        $meter = Meter::with(['tariff', 'customer'])->findOrFail($this->meterId);
        $start = CarbonImmutable::parse($this->periodStart)->startOfDay();
        $end = CarbonImmutable::parse($this->periodEnd)->startOfDay();
        $days = (int) $start->diffInDays($end) + 1;

        $usage = $invoices->usageForPeriod($meter, $start, $end);

        $lines = [
            [
                'description' => "Energy ({$meter->tariff->code})",
                'quantity' => $usage,
                'amount_cents' => $calculator->tieredEnergyCents($meter->tariff, $usage),
            ],
            [
                'description' => 'Standing charge',
                'quantity' => $days,
                'amount_cents' => $calculator->standingChargeCents($meter, $days),
            ],
        ];

        $invoice = DB::transaction(function () use ($invoices, $meter, $start, $end, $lines) {
            $invoice = Invoice::create([
                'customer_id' => $meter->customer_id,
                'meter_id' => $meter->id,
                'number' => $invoices->nextNumber(),
                'status' => Invoice::STATUS_DRAFT,
                'period_start' => $start,
                'period_end' => $end,
            ]);

            $invoices->writeLines($invoice, $lines);
            $meter->update(['last_billed_at' => $end]);

            return $invoice;
        });

        $invoices->finalize($invoice);
    }
}
