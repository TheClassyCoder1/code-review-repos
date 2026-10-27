<?php

namespace App\Jobs;

use App\Models\Installment;
use App\Services\InstallmentService;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\Middleware\WithoutOverlapping;

class ChargeInstallmentJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable;

    public int $tries = 3;

    public function __construct(public int $installmentId)
    {
    }

    /**
     * @return array<int, object>
     */
    public function middleware(): array
    {
        return [
            (new WithoutOverlapping('installment:'.$this->installmentId))->releaseAfter(30)->expireAfter(300),
        ];
    }

    public function handle(InstallmentService $installments): void
    {
        $installment = Installment::with('plan.invoice.customer')->findOrFail($this->installmentId);

        $installments->chargeIfDue($installment);
    }
}
