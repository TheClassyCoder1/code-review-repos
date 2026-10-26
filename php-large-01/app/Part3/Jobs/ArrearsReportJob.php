<?php

namespace App\Part3\Jobs;

use App\Part3\Exports\CollectionsCsvWriter;
use App\Part3\Repositories\InstallmentRepository;
use Carbon\CarbonImmutable;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;

/**
 * Weekly arrears report for the collections team: every installment that
 * fell due in the past week and is still unpaid.
 */
class ArrearsReportJob implements ShouldQueue
{
    use Dispatchable;
    use Queueable;

    public function __construct(public readonly string $weekEnding)
    {
    }

    public function handle(CollectionsCsvWriter $writer, InstallmentRepository $installments): void
    {
        $to = CarbonImmutable::parse($this->weekEnding);
        $stream = $writer->open(rtrim((string) config('part3.exports.directory'), '/') . "/arrears-{$to->format('Y-m-d')}.csv");

        try {
            foreach ($installments->dueBetween($to->subDays(6), $to) as $installment) {
                $writer->writeInstallment($stream, $installment, $to);
            }
        } finally {
            fclose($stream);
        }
    }
}
